import axios from 'axios';
import { notification } from 'antd';
import { useAuthStore } from '@/stores/auth';

// Axios 인스턴스 생성
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:7092',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: Authorization 헤더에 API 키 주입
apiClient.interceptors.request.use(
  (config) => {
    const apiKey = useAuthStore.getState().apiKey;
    if (apiKey) {
      config.headers['Authorization'] = `Bearer ${apiKey}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 응답 인터셉터: 오류 처리
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      notification.error({
        message: '인증 오류',
        description: 'API 키가 유효하지 않습니다. API 키를 확인해 주세요.',
      });
    } else if (status === 400) {
      const message = error.response?.data?.message ?? '요청이 올바르지 않습니다.';
      notification.error({
        message: '요청 오류',
        description: message,
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
