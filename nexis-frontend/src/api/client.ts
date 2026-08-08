import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '@/lib/config';
import { refreshTokenStorage } from '@/lib/refreshTokenStorage';
import { useAuthStore } from '@/store/authStore';
import type { ApiErrorBody, LoginResponse } from '@/types/api';


interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

// Requests to these paths must never trigger the refresh flow — e.g. a wrong
// password on /login returns 401 too, and that is not an "expired token" case.
const NO_RETRY_PATHS = [
  '/auth/login',
  '/auth/signup',
  '/auth/refresh',
  '/auth/forgot-password',
  '/auth/reset-password',
];

function shouldSkipRetry(url?: string): boolean {
  if (!url) return false;
  return NO_RETRY_PATHS.some((path) => url.includes(path));
}

function logRequestFailure(error: AxiosError): void {
  const method = error.config?.method?.toUpperCase() ?? '?';
  const url = `${error.config?.baseURL ?? ''}${error.config?.url ?? ''}`;
  const status = error.response?.status ?? '(no response)';
  // eslint-disable-next-line no-console
  console.error(
    `[Nexis API] ${method} ${url} → ${status}`,
    '\nresponse body:',
    error.response?.data,
    '\nrequest body:',
    error.config?.data
  );
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error || !token) reject(error);
    else resolve(token);
  });
  failedQueue = [];
}

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    logRequestFailure(error);

    const originalRequest = error.config as RetryableConfig | undefined;
    const status = error.response?.status;
    const isAuthFailure = status === 401 || status === 410;

    if (!originalRequest || !isAuthFailure || originalRequest._retry || shouldSkipRetry(originalRequest.url)) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        })
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    const refreshToken = refreshTokenStorage.get();
    if (!refreshToken) {
      isRefreshing = false;
      useAuthStore.getState().logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }

   try {
      const { data } = await axios.post<LoginResponse>('/api/auth/refresh', {
        token: refreshToken,
      });
      const currentUser = useAuthStore.getState().user;
      if (currentUser && currentUser.id !== data.id) {
        // eslint-disable-next-line no-console
        console.error('[Nexis] Refresh token identity mismatch — forcing re-login.');
        processQueue(error, null);
        refreshTokenStorage.clear();
        useAuthStore.getState().logout();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      refreshTokenStorage.set(data.refreshToken);
      useAuthStore.getState().setAccessToken(data.accessToken);

      processQueue(null, data.accessToken);
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      refreshTokenStorage.clear();
      useAuthStore.getState().logout();
      window.location.href = '/login';
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export function getErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && error.response?.data) {
    const data = error.response.data;
    
    // 1. Spring Boot Standard Error Attributes
    if (typeof data.message === 'string' && data.message.trim() !== '') {
      return data.message;
    }
    // 2. Spring Boot RFC 7807 ProblemDetail
    if (typeof data.detail === 'string' && data.detail.trim() !== '') {
      return data.detail;
    }
    // 3. Basic error string fallback
    if (typeof data.error === 'string' && data.error.trim() !== '') {
      return data.error;
    }
    // 4. Spring Boot Validation Errors (MethodArgumentNotValidException)
    if (Array.isArray(data.errors) && data.errors.length > 0 && data.errors[0].defaultMessage) {
      return data.errors[0].defaultMessage;
    }
  }
  
  if (error instanceof Error && error.message) {
    return error.message;
  }
  
  return fallback;
}
export function getErrorStatus(error: unknown): number | undefined {
  if (axios.isAxiosError(error)) return error.response?.status;
  return undefined;
}