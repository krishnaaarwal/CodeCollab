import { apiClient } from './client';
import type {
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
  UserProfile,
} from '@/types/api';

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', payload);
  return data;
}

export async function signup(payload: SignupRequest): Promise<SignupResponse> {
  const { data } = await apiClient.post<SignupResponse>('/auth/signup', payload);
  return data;
}

export async function fetchCurrentUser(): Promise<UserProfile> {
  const { data } = await apiClient.get<UserProfile>('/auth/me');
  return data;
}

export async function requestPasswordReset(email: string): Promise<void> {
  await apiClient.post('/auth/forgot-password', { email });
}

export async function submitPasswordReset(payload: {
  email: string;
  otp: string;
  newPassword: string;
}): Promise<void> {
  await apiClient.post('/auth/reset-password', payload);
}

export async function logoutRequest(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken });
}
