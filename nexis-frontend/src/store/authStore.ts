import { create } from 'zustand';
import type { UserProfile } from '@/types/api';

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  /** True until the initial silent-refresh-on-load attempt finishes. */
  isBootstrapping: boolean;

  setUser: (user: UserProfile) => void;
  setAccessToken: (token: string | null) => void;
  setBootstrapped: () => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  isBootstrapping: true,

  setUser: (user) => set({ user }),
  setAccessToken: (token) => set({ accessToken: token, isAuthenticated: !!token }),
  setBootstrapped: () => set({ isBootstrapping: false }),
  logout: () => set({ user: null, accessToken: null, isAuthenticated: false }),
}));
