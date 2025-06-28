// src/components/ALevelForm.jsx
import React from "react";
import { Grid, TextField, Typography, MenuItem } from "@mui/material";

const ALevelForm = ({ data, onChange }) => {
  const handleChange = (field) => (event) => {
    onChange(field, event.target.value);
  };

  return (
    <div style={{ marginTop: "2rem" }}>
      <Typography variant="h6" gutterBottom>
        🏫 A-Level (UACE) Information
      </Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="UACE Year Code"
            type="number"
            value={data.uace_year_code}
            onChange={handleChange("uace_year_code")}
            InputLabelProps={{ style: { color: "#ffffff" } }}
            inputProps={{ style: { color: "#ffffff" } }}
            variant="outlined"
          />
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            select
            fullWidth
            label="General Paper (0 = No, 1 = Yes)"
            value={data.general_paper}
            onChange={handleChange("general_paper")}
            InputLabelProps={{ style: { color: "#ffffff" } }}
            inputProps={{ style: { color: "#ffffff" } }}
            variant="outlined"
          >
            <MenuItem value={1}>Yes</MenuItem>
            <MenuItem value={0}>No</MenuItem>
          </TextField>
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Average Grade Weight"
            type="number"
            value={data.alevel_average_grade_weight}
            onChange={handleChange("alevel_average_grade_weight")}
            InputLabelProps={{ style: { color: "#ffffff" } }}
            inputProps={{ style: { color: "#ffffff" } }}
            variant="outlined"
          />
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Total Grade Weight"
            type="number"
            value={data.alevel_total_grade_weight}
            onChange={handleChange("alevel_total_grade_weight")}
            InputLabelProps={{ style: { color: "#ffffff" } }}
            inputProps={{ style: { color: "#ffffff" } }}
            variant="outlined"
          />
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Std. Dev. Grade Weight"
            type="number"
            value={data.alevel_std_dev_grade_weight}
            onChange={handleChange("alevel_std_dev_grade_weight")}
            InputLabelProps={{ style: { color: "#ffffff" } }}
            inputProps={{ style: { color: "#ffffff" } }}
            variant="outlined"
          />
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Dominant Grade Weight"
            type="number"
            value={data.alevel_dominant_grade_weight}
            onChange={handleChange("alevel_dominant_grade_weight")}
            InputLabelProps={{ style: { color: "#ffffff" } }}
            inputProps={{ style: { color: "#ffffff" } }}
            variant="outlined"
          />
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Count of Weak Grades"
            type="number"
            value={data.alevel_count_weak_grades}
            onChange={handleChange("alevel_count_weak_grades")}
            InputLabelProps={{ style: { color: "#ffffff" } }}
            inputProps={{ style: { color: "#ffffff" } }}
            variant="outlined"
          />
        </Grid>
      </Grid>
    </div>
  );
};

export default ALevelForm;
