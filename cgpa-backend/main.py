# cgpa-backend/main.py
from fastapi import FastAPI
from schemas.request_schema import StudentInput
from utils.feature_engineering import transform_input, classify_band
from models.model_loader import load_model
import uvicorn

app = FastAPI()
model = load_model()

@app.get("/")
def root():
    return {"message": "CGPA Prediction API is running"}

@app.post("/predict")
def predict(input_data: StudentInput):
    features = transform_input(input_data)
    prediction = model.predict([features])[0]
    return {
        "predicted_cgpa": round(prediction, 3),
        "performance_band": classify_band(prediction)
    }

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
