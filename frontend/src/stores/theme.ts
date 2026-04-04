import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { DEFAULT_THEME_KEY, getTheme } from '@/theme/themeRegistry';
import type { AppTheme } from '@/theme/types';

interface ThemeState {
  themeKey: string;
  current: AppTheme;
  setTheme: (key: string) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      themeKey: DEFAULT_THEME_KEY,
      current: getTheme(DEFAULT_THEME_KEY),
      setTheme: (key: string) => set({ themeKey: key, current: getTheme(key) }),
    }),
    {
      name: 'joins-ems-theme',
      partialize: (state) => ({ themeKey: state.themeKey }),
      merge: (persisted, current) => {
        const p = persisted as { themeKey?: string };
        const key = p?.themeKey ?? DEFAULT_THEME_KEY;
        return { ...current, themeKey: key, current: getTheme(key) };
      },
    },
  ),
);
