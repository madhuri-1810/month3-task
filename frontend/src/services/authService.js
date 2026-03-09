import api from './api';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// ─── Auth Service ─────────────────────────────────────────────────────────────
const authService = {
  register: (data) => api.post('/api/auth/register', data),
  login: (data) => api.post('/api/auth/login', data),
  logout: () => api.post('/api/auth/logout'),
  setAuthToken: (token) => { api.defaults.headers.common.Authorization = `Bearer ${token}`; },
  removeAuthToken: () => { delete api.defaults.headers.common.Authorization; },
};

export default authService;
