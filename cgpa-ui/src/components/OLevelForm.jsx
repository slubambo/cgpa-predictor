import React from 'react';
import { Grid, TextField, MenuItem, Typography } from '@mui/material';

const uceYears = Array.from({ length: 20 }, (_, i) => 2010 + i);

const OLevelForm = ({ data, onChange }) => {
  return (
    <>
      <Typography variant="h6" gutterBottom>
        📘 O-Level Academic Details
      </Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <TextField
            select
            label="UCE Year"
            fullWidth
            value={data.uce_year_code || ''}
            onChange={(e) => onChange('uce_year_code', parseInt(e.target.value))}
          >
            {uceYears.map((year) => (
              <MenuItem key={year} value={year}>
                {year}
              </MenuItem>
            ))}
          </TextField>
        </Grid>

        <Grid item xs={12} sm={6}>
          <TextField
            label="UCE Credits"
            fullWidth
            type="number"
            value={data.uce_credits || ''}
            onChange={(e) => onChange('uce_credits', parseInt(e.target.value))}
          />
        </Grid>

        {[
          ['average_olevel_grade', 'Average O-Level Grade'],
          ['best_sum_out_of_six', 'Best Sum of 6'],
          ['best_sum_out_of_eight', 'Best Sum of 8'],
          ['best_sum_out_of_ten', 'Best Sum of 10'],
          ['count_weak_grades_olevel', 'Count of Weak Grades'],
          ['highest_olevel_grade', 'Highest Grade'],
          ['lowest_olevel_grade', 'Lowest Grade'],
          ['std_dev_olevel_grade', 'Std Dev of Grades']
        ].map(([field, label]) => (
          <Grid item xs={12} sm={6} key={field}>
            <TextField
              label={label}
              fullWidth
              type="number"
              value={data[field] || ''}
              onChange={(e) => onChange(field, parseFloat(e.target.value))}
            />
          </Grid>
        ))}
      </Grid>
    </>
  );
};

export default OLevelForm;