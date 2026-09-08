import pytest
from uuid import uuid4
from app.schemas import (
    WaitTimePredictionRequest,
    NoShowPredictionRequest,
    OverbookingCalculationRequest,
)
from app.services import PredictionService, binomial_tail_probability

def test_binomial_tail_probability_edge_cases():
    # If k >= n, probability of > k is 0
    assert binomial_tail_probability(50, 50, 0.9) == 0.0
    # If show probability is 0, cannot exceed 0
    assert binomial_tail_probability(50, 10, 0.0) == 0.0
    # If show probability is 1, n > k has tail probability 1.0
    assert binomial_tail_probability(50, 40, 1.0) == 1.0

def test_overbooking_calculation_within_risk_threshold():
    service = PredictionService()
    # 50 raw capacity, 20% no-show rate (80% show rate), risk threshold 10%
    req = OverbookingCalculationRequest(
        raw_capacity=50,
        predicted_no_show_rate=0.20,
        risk_threshold=0.10,
        max_overbooking_ratio=1.35,
    )
    res = service.calculate_overbooking(req)
    
    assert res.computed_limit >= res.raw_capacity
    assert res.computed_limit <= int(res.raw_capacity * 1.35)
    # The achieved overflow risk must not exceed 10%
    assert res.overflow_risk_achieved <= 0.10
    # Estimated shows must be reasonable
    assert res.estimated_shows_at_limit <= res.computed_limit

def test_wait_time_prediction_peak_hour():
    service = PredictionService()
    office_id = uuid4()
    service_id = uuid4()
    
    # Off-peak (12:00) vs Peak (10:00)
    req_offpeak = WaitTimePredictionRequest(
        office_id=office_id,
        service_type_id=service_id,
        hour_of_day=12,
        day_of_week=2,
        queue_depth=5,
        default_duration_seconds=600,
    )
    req_peak = WaitTimePredictionRequest(
        office_id=office_id,
        service_type_id=service_id,
        hour_of_day=10,
        day_of_week=2,
        queue_depth=5,
        default_duration_seconds=600,
    )
    
    res_offpeak = service.predict_wait_time(req_offpeak)
    res_peak = service.predict_wait_time(req_peak)
    
    # Peak hour wait should be higher
    assert res_peak.predicted_wait_seconds > res_offpeak.predicted_wait_seconds
    assert res_peak.source == "HEURISTIC"
