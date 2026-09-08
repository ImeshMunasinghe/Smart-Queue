from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.schemas import (
    WaitTimePredictionRequest,
    WaitTimePredictionResponse,
    NoShowPredictionRequest,
    NoShowPredictionResponse,
    OverbookingCalculationRequest,
    OverbookingCalculationResponse,
)
from app.services import PredictionService

app = FastAPI(
    title="Smart Queue Prediction Microservice",
    version="1.0.0",
    description="Stateless intelligence layer providing wait-time estimations, no-show probabilities, and binomial overbooking bounds."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

service = PredictionService()

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "smart-queue-prediction",
        "version": "1.0.0",
        "model_status": "HEURISTIC_COLD_START_ACTIVE"
    }

@app.post("/predict/wait-time", response_model=WaitTimePredictionResponse)
def predict_wait_time(req: WaitTimePredictionRequest):
    try:
        return service.predict_wait_time(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Wait time prediction error: {str(e)}")

@app.post("/predict/no-show", response_model=NoShowPredictionResponse)
def predict_no_show(req: NoShowPredictionRequest):
    try:
        return service.predict_no_show(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"No-show prediction error: {str(e)}")

@app.post("/calculate/overbooking", response_model=OverbookingCalculationResponse)
def calculate_overbooking(req: OverbookingCalculationRequest):
    try:
        return service.calculate_overbooking(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Overbooking calculation error: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
