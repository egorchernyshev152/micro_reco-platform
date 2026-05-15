import axios from 'axios';
import { useUserStore } from '../context/userStore';

const catalogApi = axios.create({
  baseURL: import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8081',
  timeout: 8000
});

catalogApi.interceptors.request.use((config) => {
  const token = useUserStore.getState().token;
  if (token) {
    config.headers = {
      ...config.headers,
      Authorization: `Bearer ${token}`
    };
  }
  return config;
});

catalogApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      useUserStore.getState().logout();
    }
    console.error('Catalog API error', error);
    return Promise.reject(error);
  }
);

export default catalogApi;
