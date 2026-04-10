import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserInfo } from '@/types/auth';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserInfo | null;
  lastActivity: number;
  // actions
  setTokens: (accessToken: string, refreshToken: string) => void;
  setAccessToken: (accessToken: string) => void;
  setUser: (user: UserInfo) => void;
  updateActivity: () => void;
  logout: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      lastActivity: Date.now(),

      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken, lastActivity: Date.now() }),

      setAccessToken: (accessToken) =>
        set({ accessToken }),

      setUser: (user) => set({ user }),

      updateActivity: () => set({ lastActivity: Date.now() }),

      logout: () =>
        set({ accessToken: null, refreshToken: null, user: null, lastActivity: 0 }),

      isAuthenticated: () => !!get().accessToken,
    }),
    {
      name: 'joins-ems-auth',
    },
  ),
);
