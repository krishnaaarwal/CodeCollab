import { useEffect, useRef, type ReactNode } from 'react';
import axios from 'axios';
import { API_BASE_URL } from '@/lib/config';
import { refreshTokenStorage } from '@/lib/refreshTokenStorage';
import { useAuthStore } from '@/store/authStore';
import { apiClient } from '@/api/client';
import type { LoginResponse } from '@/types/api';


export function AuthProvider({ children }: { children: ReactNode }) {
  const isBootstrapping = useAuthStore((s) => s.isBootstrapping);
  const startedRef = useRef(false);

  useEffect(() => {

    if (startedRef.current) return;
    startedRef.current = true;

    async function bootstrap() {
      const refreshToken = refreshTokenStorage.get();
      if (!refreshToken) {
        useAuthStore.getState().setBootstrapped();
        return;
      }

      try {
        const { data } = await axios.post<LoginResponse>(`${API_BASE_URL}/api/auth/refresh`, {
          token: refreshToken,
        });

        refreshTokenStorage.set(data.refreshToken);
        useAuthStore.getState().setAccessToken(data.accessToken);

        const me = await apiClient.get('/api/auth/me');
        useAuthStore.getState().setUser(me.data);
      } catch {
        refreshTokenStorage.clear();
        useAuthStore.getState().logout();
      } finally {
        useAuthStore.getState().setBootstrapped();
      }
    }

    bootstrap();
  }, []);

  if (isBootstrapping) {
    return (
      <div className="boot-screen">
        <div className="boot-ascii">NEXIS</div>
        <p className="boot-text">
          resuming session<span className="cursor-blink">_</span>
        </p>
      </div>
    );
  }

  return <>{children}</>;
}
