import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  apiKey: string;
  setApiKey: (key: string) => void;
  clearApiKey: () => void;
}

// API 키 인증 스토어 (localStorage에 영속 저장)
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      apiKey: '',
      setApiKey: (key: string) => set({ apiKey: key }),
      clearApiKey: () => set({ apiKey: '' }),
    }),
    {
      name: 'joins-ems-auth',
    },
  ),
);
