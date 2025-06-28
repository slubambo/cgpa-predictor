// src/components/DemographicsForm.jsx
import React from 'react';
import { Grid, TextField, MenuItem, Typography } from '@mui/material';

const DemographicsForm = ({ data, onChange }) => {
  const handleChange = (e) => {
    const { name, value } = e.target;
    onChange(name, value);
  };

  return (
    <>
      <Typography variant="h6" gutterBottom>Demographic Details</Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <TextField
            name="age_at_entry"
            label="Age at Entry"
            type="number"
            fullWidth
            value={data.age_at_entry}
            onChange={handleChange}
          />
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            select
            name="gender"
            label="Gender"
            fullWidth
            value={data.gender}
            onChange={handleChange}
          >
            <MenuItem value={1}>Male</MenuItem>
            <MenuItem value={0}>Female</MenuItem>
          </TextField>
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            select
            name="level"
            label="Level"
            fullWidth
            value={data.level}
            onChange={handleChange}
          >
            <MenuItem value={0}>Certificate</MenuItem>
            <MenuItem value={1}>Diploma</MenuItem>
            <MenuItem value={2}>Undergraduate</MenuItem>
            <MenuItem value={3}>Postgraduate</MenuItem>
          </TextField>
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            name="year_of_entry_code"
            label="Year of Entry"
            type="number"
            fullWidth
            value={data.year_of_entry_code}
            onChange={handleChange}
          />
        </Grid>
      </Grid>
    </>
  );
};

export default DemographicsForm;