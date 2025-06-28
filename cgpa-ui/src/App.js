// src/App.js
import React, { useState } from "react";
import { Container, Typography, Paper } from "@mui/material";
import DemographicsForm from "./components/DemographicsForm";
import OLevelForm from "./components/OLevelForm";

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
  });

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
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
      </Paper>
    </Container>
  );
}

export default App;
