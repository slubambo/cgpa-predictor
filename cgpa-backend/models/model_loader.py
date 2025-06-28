import joblib
import os

# ✅ Update path to use the actual model file inside model_artifacts/
MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "model_artifacts", "best_cgpa_model.pkl")

class MockModel:
    def predict(self, X):
        return [3.75 for _ in X]  # Fixed CGPA prediction for testing

def load_model():
    if os.path.exists(MODEL_PATH):
        print(f"✅ Loaded model from: {MODEL_PATH}")
        return joblib.load(MODEL_PATH)
    print("⚠️ No model file found. Using mock model for testing.")
    return MockModel()
