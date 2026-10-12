import { request } from './httpClient';
import type { AuthResponse, User } from './types';

/** AuthController (F05). */
export const authApi = {
  login: (username: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: { username, password } }),
  register: (username: string, password: string) =>
    request<User>('/auth/register', { method: 'POST', body: { username, password } }),
};
