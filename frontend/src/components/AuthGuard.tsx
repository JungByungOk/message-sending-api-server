import { useEffect, useCallback } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth';

const INACTIVITY_TIMEOUT = 30 * 60 * 1000; // 30분

export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const { accessToken, lastActivity, logout, updateActivity } = useAuthStore();

  // 비활동 타임아웃 체크
  const checkInactivity = useCallback(() => {
    if (accessToken && Date.now() - lastActivity > INACTIVITY_TIMEOUT) {
      logout();
      window.location.href = '/login';
    }
  }, [accessToken, lastActivity, logout]);

  // 사용자 활동 감지
  useEffect(() => {
    if (!accessToken) return;

    const handleActivity = () => updateActivity();
    const events = ['mousedown', 'keydown', 'scroll', 'touchstart'];
    events.forEach((event) => document.addEventListener(event, handleActivity));

    const interval = setInterval(checkInactivity, 60 * 1000); // 1분마다 체크

    return () => {
      events.forEach((event) => document.removeEventListener(event, handleActivity));
      clearInterval(interval);
    };
  }, [accessToken, updateActivity, checkInactivity]);

  if (!accessToken) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
