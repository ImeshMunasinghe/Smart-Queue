import math
from typing import Tuple
from app.schemas import (
    WaitTimePredictionRequest,
    WaitTimePredictionResponse,
    NoShowPredictionRequest,
    NoShowPredictionResponse,
    OverbookingCalculationRequest,
    OverbookingCalculationResponse,
)

def binomial_tail_probability(n: int, k_threshold: int, p_success: float) -> float:
    """
    Computes P(X > k_threshold) where X ~ Binomial(n, p_success).
    p_success is the probability that an issued token shows up.
    """
    if n <= k_threshold:
        return 0.0
    if p_success <= 0.0:
        return 0.0
    if p_success >= 1.0:
        return 1.0 if n > k_threshold else 0.0

    # Sum P(X = k) for k = k_threshold + 1 to n
    tail_prob = 0.0
    for k in range(k_threshold + 1, n + 1):
        coeff = math.comb(n, k)
        prob = coeff * (p_success ** k) * ((1.0 - p_success) ** (n - k))
        tail_prob += prob
    return min(1.0, max(0.0, tail_prob))

class PredictionService:
    def predict_wait_time(self, req: WaitTimePredictionRequest) -> WaitTimePredictionResponse:
        """
        Rule-based heuristic wait-time prediction (Pilot mode / Cold-start fallback).
        Calculates ETA based on queue depth and base service duration,
        with peak-hour adjustment factor.
        """
        base_service_time = req.default_duration_seconds
        
        # Peak-hour factor (e.g., 09:00 - 11:30 and 13:30 - 15:00 tend to take 10-15% longer)
        peak_multiplier = 1.0
        if 9 <= req.hour_of_day <= 11 or 13 <= req.hour_of_day <= 15:
            peak_multiplier = 1.15
        
        unit_wait = int(base_service_time * peak_multiplier)
        total_wait = unit_wait * max(1, req.queue_depth)

        # Confidence intervals
        p50 = total_wait
        p90 = int(total_wait * 1.25)

        return WaitTimePredictionResponse(
            predicted_wait_seconds=total_wait,
            confidence_p50=p50,
            confidence_p90=p90,
            source="HEURISTIC"
        )

    def predict_no_show(self, req: NoShowPredictionRequest) -> NoShowPredictionResponse:
        """
        Rule-based heuristic no-show prediction (Pilot mode).
        Weighted combination of citizen historical no-show rate and lead-time factors.
        """
        base_rate = 0.15  # Baseline average no-show rate for OPD/Gov counters
        
        # If citizen has an established history, weight it 60%
        history_weight = req.citizen_history_no_show_rate * 0.6 + base_rate * 0.4
        
        # High lead time (>2 hours) increases no-show likelihood
        lead_time_factor = 1.0
        if req.lead_time_minutes > 180:
            lead_time_factor = 1.25
        elif req.lead_time_minutes < 30:
            lead_time_factor = 0.8  # Citizens arriving quickly are more likely to show
            
        prob = min(0.85, max(0.02, history_weight * lead_time_factor))
        
        return NoShowPredictionResponse(
            no_show_probability=round(prob, 3),
            source="HEURISTIC"
        )

    def calculate_overbooking(self, req: OverbookingCalculationRequest) -> OverbookingCalculationResponse:
        """
        Exact Binomial distribution overbooking calculation.
        Finds the largest N >= raw_capacity such that P(shows > raw_capacity) <= risk_threshold.
        """
        raw_cap = req.raw_capacity
        p_show = 1.0 - req.predicted_no_show_rate
        alpha = req.risk_threshold
        max_allowed_n = int(raw_cap * req.max_overbooking_ratio)

        # If show rate is 100%, we cannot overbook at all
        if p_show >= 0.999:
            return OverbookingCalculationResponse(
                raw_capacity=raw_cap,
                computed_limit=raw_cap,
                predicted_no_show_rate=req.predicted_no_show_rate,
                risk_threshold=alpha,
                estimated_shows_at_limit=float(raw_cap),
                overflow_risk_achieved=0.0
            )

        best_n = raw_cap
        best_overflow_risk = 0.0

        for n in range(raw_cap, max_allowed_n + 1):
            overflow_prob = binomial_tail_probability(n, raw_cap, p_show)
            if overflow_prob <= alpha:
                best_n = n
                best_overflow_risk = overflow_prob
            else:
                # Exceeded the risk threshold
                break

        estimated_shows = round(best_n * p_show, 1)

        return OverbookingCalculationResponse(
            raw_capacity=raw_cap,
            computed_limit=best_n,
            predicted_no_show_rate=req.predicted_no_show_rate,
            risk_threshold=alpha,
            estimated_shows_at_limit=estimated_shows,
            overflow_risk_achieved=round(best_overflow_risk, 4)
        )
