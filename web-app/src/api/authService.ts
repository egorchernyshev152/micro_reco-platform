import catalogApi from './catalogClient';
import { AuthUser } from '../context/userStore';

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: AuthUser;
}

export const authService = {
  login: (payload: { email: string; password: string }) =>
    catalogApi.post<AuthResponse>('/auth/login', payload).then((res) => res.data),

  register: (payload: { name: string; email: string; password: string }) =>
    catalogApi.post<AuthResponse>('/auth/register', payload).then((res) => res.data),

  me: () => catalogApi.get<AuthUser>('/auth/me').then((res) => res.data)
};
