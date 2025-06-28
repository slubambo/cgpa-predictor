// src/theme.js
import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'dark',
    background: {
      default: '#1e1e1e',
      paper: '#2a2a2a',
    },
    primary: {
      main: '#10a37f',
    },
    secondary: {
      main: '#8b5cf6',
    },
    text: {
      primary: '#ffffff',
      secondary: '#cfcfcf',
    },
  },
  typography: {
    fontFamily: ['"Segoe UI"', 'Roboto', 'Helvetica', 'Arial', 'sans-serif'].join(','),
  },
});

export default theme;
