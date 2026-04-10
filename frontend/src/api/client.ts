import axios from 'axios';
import { notification } from 'antd';
import { useAuthStore } from '@/stores/auth';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:7092',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: JWT accessToken 주입
apiClient.interceptors.request.use(
  (config) => {
    const { accessToken } = useAuthStore.getState();
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 토큰 갱신 중복 방지
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (token) {
      prom.resolve(token);
    } else {
      prom.reject(error);
    }
  });
  failedQueue = [];
};

// 응답 인터셉터: 401 시 토큰 갱신 시도
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;

    // 401이고 refresh 요청이 아닌 경우 토큰 갱신 시도
    if (status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers['Authorization'] = `Bearer ${token}`;
              resolve(apiClient(originalRequest));
            },
            reject,
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const { refreshToken, logout, setAccessToken } = useAuthStore.getState();

      if (!refreshToken) {
        logout();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const response = await axios.post(
          `${apiClient.defaults.baseURL}/auth/refresh`,
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } },
        );
        const newAccessToken = response.data.accessToken;
        setAccessToken(newAccessToken);
        processQueue(null, newAccessToken);
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    if (status === 400) {
      const message = error.response?.data?.message ?? '요청이 올바르지 않습니다.';
      notification.error({
        message: '요청 오류',
        description: message,
      });
    } else if (status === 403) {
      notification.error({
        message: '권한 없음',
        description: '해당 작업에 대한 권한이 없습니다.',
      });
    } else if (status >= 500) {
      notification.error({
        message: '서버 오류',
        description: '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
      });
    }

    return Promise.reject(error);
  },
);

export default apiClient;
