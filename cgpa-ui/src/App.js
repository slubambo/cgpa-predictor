// src/App.js
import React, { useState } from 'react';
import { Container, Typography, Paper } from '@mui/material';
import DemographicsForm from './components/DemographicsForm';

function App() {
  const [formData, setFormData] = useState({
    age_at_entry: '',
    gender: '',
    level: '',
    year_of_entry_code: ''
  });

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <Container maxWidth="md" style={{ marginTop: '2rem' }}>
      <Paper elevation={3} style={{ padding: '2rem', backgroundColor: '#1e1e1e', color: '#ffffff' }}>
        <Typography variant="h4" gutterBottom align="center">
          🎓 CGPA Prediction Form
        </Typography>
        <DemographicsForm data={formData} onChange={handleFormChange} />
      </Paper>
    </Container>
  );
}

export default App;
