// src/components/InstitutionalForm.jsx
import React from 'react';
import { Grid, TextField, MenuItem, Typography } from '@mui/material';

const campusOptions = [
  { label: 'Main Campus', value: 0 },
  { label: 'Kampala Campus', value: 1 },
  { label: 'Mbale Campus', value: 2 }
];

const programOptions = [
  { label: 'Bachelor of Information Technology', value: 101 },
  { label: 'Bachelor of Business Administration', value: 102 },
  { label: 'Bachelor of Education', value: 103 }
];

const curriculumOptions = [
  { label: 'IT Curriculum 2020', value: 204 },
  { label: 'BBA Curriculum 2019', value: 205 },
  { label: 'Education Curriculum 2021', value: 206 }
];

function InstitutionalForm({ data, onChange }) {
  return (
    <div style={{ marginTop: '2rem' }}>
      <Typography variant="h6" gutterBottom>
        🏫 Institutional Placement
      </Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <TextField
            select
            fullWidth
            label="Campus"
            value={data.campus_id_code}
            onChange={(e) => onChange('campus_id_code', e.target.value)}
            variant="outlined"
            InputLabelProps={{ style: { color: '#ccc' } }}
            SelectProps={{ style: { color: '#fff' } }}
          >
            {campusOptions.map((option) => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            select
            fullWidth
            label="Program"
            value={data.program_id_code}
            onChange={(e) => onChange('program_id_code', e.target.value)}
            variant="outlined"
            InputLabelProps={{ style: { color: '#ccc' } }}
            SelectProps={{ style: { color: '#fff' } }}
          >
            {programOptions.map((option) => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            select
            fullWidth
            label="Curriculum"
            value={data.curriculum_id_code}
            onChange={(e) => onChange('curriculum_id_code', e.target.value)}
            variant="outlined"
            InputLabelProps={{ style: { color: '#ccc' } }}
            SelectProps={{ style: { color: '#fff' } }}
          >
            {curriculumOptions.map((option) => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
      </Grid>
    </div>
  );
}

export default InstitutionalForm;
