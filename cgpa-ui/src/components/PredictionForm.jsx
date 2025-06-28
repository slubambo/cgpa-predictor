// src/components/PredictionForm.jsx
import React from 'react';
import { Box, Typography, Paper } from '@mui/material';
import DemographicsForm from './DemographicsForm';
import OLevelForm from './OLevelForm'

const PredictionForm = () => {
  return (
    <Box sx={{ mt: 4, mb: 6 }}>
      <Paper elevation={3} sx={{ p: 4, borderRadius: 3 }}>
        <Typography variant="h5" gutterBottom>
          🎓 CGPA Prediction Form
        </Typography>

        {/* Step 1: Demographic Info */}
        <DemographicsForm />

        {/* Steps 2–4: Other forms will be added below */}
        <OLevelForm />
        {/* <ALevelForm /> */}
        {/* <InstitutionalForm /> */}
      </Paper>
    </Box>
  );
};

export default PredictionForm;