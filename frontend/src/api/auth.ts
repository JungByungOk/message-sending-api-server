import apiClient from './client';
import type { LoginRequest, LoginResponse, TokenResponse, ChangePasswordRequest, UserInfo, CreateUserRequest, UpdateUserRequest } from '@/types/auth';

// login은 인터셉터 없이 직접 호출 (토큰 없는 상태)
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:7092';

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<LoginResponse>('/auth/login', data),

  refresh: (refreshToken: string) =>
    apiClient.post<TokenResponse>('/auth/refresh', { refreshToken }),

  logout: () =>
    apiClient.post('/auth/logout'),

  changePassword: (data: ChangePasswordRequest) =>
    apiClient.post('/auth/change-password', data),

  getMe: () =>
    apiClient.get<UserInfo>('/users/me'),

  getUsers: () =>
    apiClient.get<UserInfo[]>('/users'),

  createUser: (data: CreateUserRequest) =>
    apiClient.post<UserInfo>('/users', data),

  updateUser: (userId: number, data: UpdateUserRequest) =>
    apiClient.put<UserInfo>(`/users/${userId}`, data),

  deleteUser: (userId: number) =>
    apiClient.delete(`/users/${userId}`),
};
