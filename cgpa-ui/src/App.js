// src/App.js
import React, { useState } from "react";
import axios from "axios";
import {
  Container,
  Typography,
  Paper,
  Button,
  CircularProgress,
  Alert,
} from "@mui/material";
import DemographicsForm from "./components/DemographicsForm";
import OLevelForm from "./components/OLevelForm";
import ALevelForm from "./components/ALevelForm";
import InstitutionalForm from "./components/InstitutionalForm";

function App() {
  const [formData, setFormData] = useState({
    age_at_entry: "",
    gender: "",
    level: "",
    year_of_entry_code: "",
    uce_year_code: "",
    uce_credits: "",
    average_olevel_grade: "",
    best_sum_out_of_six: "",
    best_sum_out_of_eight: "",
    best_sum_out_of_ten: "",
    count_weak_grades_olevel: "",
    highest_olevel_grade: "",
    lowest_olevel_grade: "",
    std_dev_olevel_grade: "",
    uace_year_code: "",
    general_paper: "",
    alevel_average_grade_weight: "",
    alevel_total_grade_weight: "",
    alevel_std_dev_grade_weight: "",
    alevel_dominant_grade_weight: "",
    alevel_count_weak_grades: "",
    campus_id_code: "",
    program_id_code: "",
    curriculum_id_code: "",
    high_school_performance_variance: "",
    high_school_performance_stability_index: "",
  });

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    const payload = {
      ...formData,
      age_at_entry: Number(formData.age_at_entry),
      gender: Number(formData.gender),
      level: Number(formData.level),
      uce_year_code: Number(formData.uce_year_code),
      uce_credits: Number(formData.uce_credits),
      average_olevel_grade: parseFloat(formData.average_olevel_grade),
      best_sum_out_of_six: Number(formData.best_sum_out_of_six),
      best_sum_out_of_eight: Number(formData.best_sum_out_of_eight),
      best_sum_out_of_ten: Number(formData.best_sum_out_of_ten),
      count_weak_grades_olevel: Number(formData.count_weak_grades_olevel),
      highest_olevel_grade: Number(formData.highest_olevel_grade),
      lowest_olevel_grade: Number(formData.lowest_olevel_grade),
      std_dev_olevel_grade: parseFloat(formData.std_dev_olevel_grade),
      uace_year_code: Number(formData.uace_year_code),
      general_paper: Number(formData.general_paper),
      alevel_average_grade_weight: parseFloat(
        formData.alevel_average_grade_weight
      ),
      alevel_total_grade_weight: parseFloat(formData.alevel_total_grade_weight),
      alevel_std_dev_grade_weight: parseFloat(
        formData.alevel_std_dev_grade_weight
      ),
      alevel_dominant_grade_weight: parseFloat(
        formData.alevel_dominant_grade_weight
      ),
      alevel_count_weak_grades: Number(formData.alevel_count_weak_grades),
      year_of_entry_code: Number(formData.year_of_entry_code),
      campus_id_code: Number(formData.campus_id_code),
      program_id_code: Number(formData.program_id_code),
      curriculum_id_code: Number(formData.curriculum_id_code),
      high_school_performance_variance: parseFloat(
        formData.high_school_performance_variance
      ),
      high_school_performance_stability_index: parseFloat(
        formData.high_school_performance_stability_index
      ),
    };

    // ✅ Collect all missing or invalid fields
    const missingFields = Object.entries(payload)
      .filter(([key, val]) => val === "" || val === null || Number.isNaN(val))
      .map(([key]) => key);

    if (missingFields.length > 0) {
      alert(
        `❗ Please fill in the following required fields:\n\n- ${missingFields.join(
          "\n- "
        )}`
      );
      return;
    }

    console.log("📤 Sending payload to backend:", payload);
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await axios.post(
        "http://localhost:8000/predict",
        payload
      );
      setResult(response.data);
    } catch (err) {
      console.error("❌ API Error:", err);
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md" style={{ marginTop: "2rem" }}>
      <Paper
        elevation={3}
        style={{
          padding: "2rem",
          backgroundColor: "#1e1e1e",
          color: "#ffffff",
        }}
      >
        <Typography variant="h4" gutterBottom align="center">
          🎓 CGPA Prediction Form
        </Typography>

        <DemographicsForm data={formData} onChange={handleFormChange} />
        <OLevelForm data={formData} onChange={handleFormChange} />
        <ALevelForm data={formData} onChange={handleFormChange} />
        <InstitutionalForm data={formData} onChange={handleFormChange} />

        <div style={{ textAlign: "center", marginTop: "2rem" }}>
          <Button
            variant="contained"
            color="primary"
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Predict CGPA"
            )}
          </Button>
        </div>

        {result && (
          <Alert
            severity="success"
            style={{
              marginTop: "2rem",
              backgroundColor: "#2d2d2d",
              color: "#00ff95",
              lineHeight: "1.8",
            }}
          >
            🤖 <strong>Predicted CGPA:</strong> {result.predicted_cgpa} <br />
            🧠 <strong>Performance Band:</strong> {result.performance_band}
          </Alert>
        )}

        {error && (
          <Alert severity="error" style={{ marginTop: "2rem" }}>
            {error}
          </Alert>
        )}
      </Paper>
    </Container>
  );
}

export default App;
