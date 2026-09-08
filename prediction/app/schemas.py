from pydantic import BaseModel, Field
from typing import Optional
from uuid import UUID

class WaitTimePredictionRequest(BaseModel):
    office_id: UUID
    counter_id: Optional[UUID] = None
    service_type_id: UUID
    hour_of_day: int = Field(..., ge=0, le=23)
    day_of_week: int = Field(..., ge=1, le=7)
    queue_depth: int = Field(0, ge=0)
    default_duration_seconds: int = Field(900, ge=30)

class WaitTimePredictionResponse(BaseModel):
    predicted_wait_seconds: int
    confidence_p50: int
    confidence_p90: int
    source: str = "HEURISTIC"  # "MODEL" | "HEURISTIC"

class NoShowPredictionRequest(BaseModel):
    office_id: UUID
    service_type_id: UUID
    citizen_history_no_show_rate: float = Field(0.15, ge=0.0, le=1.0)
    slot_hour: int = Field(..., ge=0, le=23)
    lead_time_minutes: int = Field(..., ge=0)
    day_of_week: int = Field(..., ge=1, le=7)

class NoShowPredictionResponse(BaseModel):
    no_show_probability: float
    source: str = "HEURISTIC"

class OverbookingCalculationRequest(BaseModel):
    raw_capacity: int = Field(..., gt=0)
    predicted_no_show_rate: float = Field(..., ge=0.0, le=0.99)
    risk_threshold: float = Field(0.10, gt=0.0, lt=0.50)  # Default 10%
    max_overbooking_ratio: float = Field(1.35, ge=1.0, le=2.0)  # Capped at 35% overbooking by default

class OverbookingCalculationResponse(BaseModel):
    raw_capacity: int
    computed_limit: int
    predicted_no_show_rate: float
    risk_threshold: float
    estimated_shows_at_limit: float
    overflow_risk_achieved: float
