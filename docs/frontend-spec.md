# Joins EMS 프론트엔드 어드민 대시보드 기술 명세서

> 작성일: 2026-04-04
> 대상 프로젝트: Joins EMS Admin Dashboard (React SPA)

---

## 1. 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| React | 19.2 | UI 프레임워크 |
| TypeScript | 5.9 | 타입 안전성 |
| Vite | 8 | 빌드 도구 |
| Ant Design | 5.29 | UI 컴포넌트 |
| @ant-design/pro-components | 2.8 | ProLayout, ProTable |
| @ant-design/icons | 6.1 | 아이콘 |
| TanStack React Query | 5 | 서버 상태 관리 |
| Zustand | 5 | 클라이언트 상태 관리 |
| React Router DOM | 7 | 라우팅 |
| Axios | 1.14 | HTTP 클라이언트 |
| Day.js | 1.11 | 날짜 처리 |
| ESLint | 9 | 코드 린트 |

---

## 2. 디렉토리 구조

```
frontend/
├── public/
├── src/
│   ├── api/                  # API 클라이언트 및 엔드포인트 함수
│   │   ├── client.ts         # Axios 인스턴스 설정
│   │   ├── tenant.ts         # 테넌트 API 함수
│   │   ├── email.ts          # 이메일 발송 API 함수
│   │   ├── template.ts       # 템플릿 API 함수
│   │   ├── scheduler.ts      # 스케줄러 API 함수
│   │   ├── onboarding.ts     # 온보딩 API 함수
│   │   └── suppression.ts    # Suppression API 함수
│   ├── components/           # 공용 컴포넌트
│   │   ├── ErrorBoundary.tsx
│   │   ├── PageContainer.tsx
│   │   └── StatusBadge.tsx
│   ├── hooks/                # TanStack Query를 래핑한 커스텀 훅
│   │   ├── useTenant.ts
│   │   ├── useEmail.ts
│   │   ├── useTemplate.ts
│   │   ├── useScheduler.ts
│   │   └── useOnboarding.ts
│   ├── layouts/              # ProLayout 기반 레이아웃
│   │   ├── MainLayout.tsx
│   │   └── menuConfig.ts
│   ├── pages/                # 기능별 페이지 컴포넌트
│   │   ├── dashboard/
│   │   │   └── DashboardPage.tsx
│   │   ├── tenant/
│   │   │   ├── TenantListPage.tsx
│   │   │   ├── TenantCreatePage.tsx
│   │   │   └── TenantDetailPage.tsx
│   │   ├── email/
│   │   │   ├── EmailSendPage.tsx
│   │   │   └── EmailHistoryPage.tsx
│   │   ├── template/
│   │   │   ├── TemplateListPage.tsx
│   │   │   ├── TemplateCreatePage.tsx
│   │   │   └── TemplateEditPage.tsx
│   │   ├── scheduler/
│   │   │   ├── SchedulerListPage.tsx
│   │   │   └── SchedulerCreatePage.tsx
│   │   ├── onboarding/
│   │   │   ├── OnboardingWizardPage.tsx
│   │   │   └── OnboardingStatusPage.tsx
│   │   └── suppression/
│   │       └── SuppressionPage.tsx
│   ├── stores/               # Zustand 스토어
│   │   ├── authStore.ts
│   │   └── uiStore.ts
│   ├── types/                # TypeScript 인터페이스 및 타입
│   │   ├── tenant.ts
│   │   ├── email.ts
│   │   ├── scheduler.ts
│   │   ├── onboarding.ts
│   │   └── suppression.ts
│   ├── utils/                # 유틸리티 함수
│   │   ├── format.ts
│   │   └── validation.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── router.tsx
├── .env.development
├── .env.production
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

---

## 3. API 연동 타입 정의

### 3.1 공통 타입

```typescript
// src/types/common.ts

/** 페이지네이션 응답 공통 래퍼 */
interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/** API 에러 응답 */
interface ApiError {
  status: number;
  code: string;
  message: string;
  timestamp: string;
}
```

---

### 3.2 테넌트 API 타입

```typescript
// src/types/tenant.ts

type VerificationStatus = 'PENDING' | 'VERIFIED' | 'FAILED';
type TenantStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

interface Tenant {
  tenantId: string;
  tenantName: string;
  domain: string;
  apiKey: string;
  configSetName: string | null;
  verificationStatus: VerificationStatus;
  quotaDaily: number;
  quotaMonthly: number;
  status: TenantStatus;
  createdAt: string;
  updatedAt: string;
}

interface CreateTenantRequest {
  tenantName: string;
  domain: string;
  configSetName?: string;
  quotaDaily: number;
  quotaMonthly: number;
}

interface UpdateTenantRequest {
  tenantName?: string;
  configSetName?: string | null;
  quotaDaily?: number;
  quotaMonthly?: number;
  status?: TenantStatus;
}

type TenantListResponse = PagedResponse<Tenant>;
```

---

### 3.3 SES (이메일 발송) API 타입

```typescript
// src/types/email.ts

interface MessageTag {
  name: string;
  value: string;
}

/** 일반 이메일 발송 요청 */
interface SendEmailRequest {
  from: string;
  to: string[];
  subject: string;
  body: string;
  tags?: MessageTag[];
}

/** 일반 이메일 발송 응답 */
interface SendEmailResponse {
  messageId: string;
  status: string;
  sentAt: string;
}

/** 템플릿 이메일 발송 요청 */
interface SendTemplatedEmailRequest {
  templateName: string;
  from: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  templateData: Record<string, string>;
  tags?: MessageTag[];
}

/** 템플릿 이메일 발송 응답 */
interface SendTemplatedEmailResponse {
  messageId: string;
  status: string;
  sentAt: string;
}

/** SES 이메일 템플릿 */
interface EmailTemplate {
  templateName: string;
  subjectPart: string;
  htmlPart: string;
  textPart?: string;
  createdAt?: string;
  updatedAt?: string;
}

interface CreateTemplateRequest {
  templateName: string;
  subjectPart: string;
  htmlPart: string;
  textPart?: string;
}

interface UpdateTemplateRequest {
  subjectPart?: string;
  htmlPart?: string;
  textPart?: string;
}

type TemplateListResponse = EmailTemplate[];
```

---

### 3.4 스케줄러 API 타입

```typescript
// src/types/scheduler.ts

type JobStatus = 'SCHEDULED' | 'RUNNING' | 'PAUSED' | 'COMPLETE' | 'ERROR' | 'BLOCKED';

/** 수신자별 템플릿 데이터를 포함한 이메일 항목 */
interface TemplatedEmailItem {
  to: string;
  templateData: Record<string, string>;
}

/** 예약 발송 작업 생성 요청 */
interface ScheduleJobRequest {
  jobName: string;
  jobGroup: string;
  description?: string;
  startDateAt: string;           // ISO 8601 형식 (예: "2026-04-10T09:00:00")
  templateName: string;
  from: string;
  templatedEmailList: TemplatedEmailItem[];
  tags?: MessageTag[];
}

/** 예약 작업 정보 */
interface JobInfo {
  jobName: string;
  groupName: string;
  jobStatus: JobStatus;
  scheduleTime: string;
  lastFiredTime: string | null;
  nextFireTime: string | null;
  description?: string;
}

/** 전체 작업 목록 응답 */
interface AllJobsResponse {
  numOfAllJobs: number;
  numOfGroups: number;
  numOfRunningJobs: number;
  jobs: JobInfo[];
}

/** 작업 일시정지/재개 요청 */
interface JobActionRequest {
  jobName: string;
  jobGroup: string;
}
```

---

### 3.5 온보딩 API 타입 (개발 예정)

```typescript
// src/types/onboarding.ts

type OnboardingStepStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

interface OnboardingStep {
  stepName: string;
  status: OnboardingStepStatus;
  completedAt?: string;
  errorMessage?: string;
}

/** 온보딩 시작 요청 */
interface OnboardingStartRequest {
  tenantName: string;
  domain: string;
  contactEmail: string;
}

/** 온보딩 시작 응답 */
interface OnboardingStartResponse {
  tenantId: string;
  domain: string;
  message: string;
}

/** DKIM DNS 레코드 */
interface DkimRecord {
  name: string;
  type: string;
  value: string;
}

/** 도메인 검증 상태 응답 */
interface DomainVerificationResponse {
  domain: string;
  verificationStatus: VerificationStatus;
  dkimRecords: DkimRecord[];
  spfRecord?: string;
}

/** 온보딩 전체 상태 */
interface OnboardingStatus {
  tenantId: string;
  domain: string;
  steps: OnboardingStep[];
  verificationStatus: VerificationStatus;
  tenantStatus: TenantStatus;
  createdAt: string;
  updatedAt: string;
}
```

---

### 3.6 쿼터 및 Suppression 타입 (개발 예정)

```typescript
// src/types/suppression.ts

interface QuotaUsage {
  limit: number;
  used: number;
  remaining: number;
}

/** 테넌트 쿼터 정보 */
interface QuotaInfo {
  tenantId: string;
  daily: QuotaUsage;
  monthly: QuotaUsage;
  updatedAt: string;
}

type SuppressionReason = 'BOUNCE' | 'COMPLAINT' | 'MANUAL';

/** Suppression 목록 항목 */
interface SuppressionEntry {
  email: string;
  reason: SuppressionReason;
  createdAt: string;
}

/** Suppression 추가 요청 */
interface AddSuppressionRequest {
  email: string;
  reason: SuppressionReason;
}

type SuppressionListResponse = PagedResponse<SuppressionEntry>;
```

---

## 4. 상태 관리 전략

### 4.1 TanStack Query (서버 상태)

모든 API 호출은 TanStack Query v5로 관리한다. 서버에서 가져오는 데이터(테넌트 목록, 템플릿 목록, 작업 현황 등)는 컴포넌트 로컬 state가 아닌 Query Cache에 보관한다.

#### queryKey 네이밍 컨벤션

```typescript
// 리소스 단위로 계층 구조 사용
['tenants']                          // 테넌트 목록
['tenants', tenantId]                // 특정 테넌트
['tenants', tenantId, 'quota']       // 테넌트 쿼터
['templates']                        // 템플릿 목록
['templates', templateName]          // 특정 템플릿
['scheduler', 'jobs']                // 스케줄러 작업 목록
['scheduler', 'jobs', jobName]       // 특정 작업
['onboarding', tenantId]             // 온보딩 상태
['suppression']                      // Suppression 목록
```

#### Mutation 후 Invalidation 전략

```typescript
// 예시: 테넌트 생성 후 목록 무효화
const createTenant = useMutation({
  mutationFn: (data: CreateTenantRequest) => tenantApi.create(data),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['tenants'] });
  },
});

// 예시: 템플릿 수정 후 해당 항목 및 목록 무효화
const updateTemplate = useMutation({
  mutationFn: ({ name, data }: { name: string; data: UpdateTemplateRequest }) =>
    templateApi.update(name, data),
  onSuccess: (_, { name }) => {
    queryClient.invalidateQueries({ queryKey: ['templates'] });
    queryClient.invalidateQueries({ queryKey: ['templates', name] });
  },
});
```

#### 기본 캐시 설정

```typescript
// src/main.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,    // 5분: 서버 상태가 5분 이내에 자주 변하지 않는 리소스
      gcTime: 1000 * 60 * 10,      // 10분: 캐시 보관 기간
      retry: 2,                    // 실패 시 최대 2회 재시도
      refetchOnWindowFocus: false, // 포커스 복귀 시 자동 재조회 비활성화
    },
  },
});
```

#### Error Boundary 연동

```typescript
// src/components/ErrorBoundary.tsx 와 연동
// useQuery의 throwOnError 옵션으로 에러를 ErrorBoundary로 전파
const { data } = useQuery({
  queryKey: ['tenants'],
  queryFn: tenantApi.list,
  throwOnError: true,  // ErrorBoundary가 처리
});
```

---

### 4.2 Zustand (클라이언트 상태)

UI 상태 및 인증 정보는 Zustand v5로 관리한다. 서버 캐시와 무관한 순수 클라이언트 상태만 보관한다.

#### authStore

```typescript
// src/stores/authStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  apiKey: string | null;
  currentTenantId: string | null;
  currentTenantName: string | null;
  setApiKey: (key: string) => void;
  setCurrentTenant: (id: string, name: string) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      apiKey: null,
      currentTenantId: null,
      currentTenantName: null,
      setApiKey: (key) => set({ apiKey: key }),
      setCurrentTenant: (id, name) =>
        set({ currentTenantId: id, currentTenantName: name }),
      clearAuth: () =>
        set({ apiKey: null, currentTenantId: null, currentTenantName: null }),
    }),
    { name: 'ems-auth' }  // localStorage 키
  )
);
```

#### uiStore

```typescript
// src/stores/uiStore.ts
import { create } from 'zustand';

type Theme = 'light' | 'dark';

interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  description?: string;
}

interface UiState {
  sidebarCollapsed: boolean;
  theme: Theme;
  notifications: Notification[];
  setSidebarCollapsed: (collapsed: boolean) => void;
  toggleSidebar: () => void;
  setTheme: (theme: Theme) => void;
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  removeNotification: (id: string) => void;
}

export const useUiStore = create<UiState>()((set) => ({
  sidebarCollapsed: false,
  theme: 'light',
  notifications: [],
  setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
  toggleSidebar: () =>
    set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  setTheme: (theme) => set({ theme }),
  addNotification: (notification) =>
    set((state) => ({
      notifications: [
        ...state.notifications,
        { ...notification, id: crypto.randomUUID() },
      ],
    })),
  removeNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    })),
}));
```

---

## 5. API 클라이언트 설정

```typescript
// src/api/client.ts
import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/authStore';

const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/** 요청 인터셉터: API Key 자동 주입 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { apiKey } = useAuthStore.getState();
    if (apiKey) {
      config.headers['X-API-Key'] = apiKey;
    }

    if (import.meta.env.DEV) {
      console.debug(`[API] ${config.method?.toUpperCase()} ${config.url}`, {
        params: config.params,
        data: config.data,
      });
    }

    return config;
  },
  (error) => Promise.reject(error)
);

/** 응답 인터셉터: 에러 처리 */
apiClient.interceptors.response.use(
  (response) => {
    if (import.meta.env.DEV) {
      console.debug(`[API] Response ${response.status}`, response.data);
    }
    return response;
  },
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status;

    switch (status) {
      case 401:
        // API Key 인증 실패 - 인증 정보 초기화
        useAuthStore.getState().clearAuth();
        break;
      case 403:
        console.error('[API] 접근 권한 없음:', error.response?.data?.message);
        break;
      case 500:
        console.error('[API] 서버 내부 오류:', error.response?.data?.message);
        break;
      default:
        if (import.meta.env.DEV) {
          console.error('[API] 오류:', error.response?.data);
        }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 6. 라우팅 구조

React Router v7의 `createBrowserRouter`를 사용한다.

```typescript
// src/router.tsx
import { createBrowserRouter } from 'react-router';
import MainLayout from '@/layouts/MainLayout';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        lazy: () => import('@/pages/dashboard/DashboardPage'),
      },

      // 테넌트 관리
      {
        path: 'tenant',
        lazy: () => import('@/pages/tenant/TenantListPage'),
      },
      {
        path: 'tenant/create',
        lazy: () => import('@/pages/tenant/TenantCreatePage'),
      },
      {
        path: 'tenant/:id',
        lazy: () => import('@/pages/tenant/TenantDetailPage'),
      },

      // 이메일 발송
      {
        path: 'email/send',
        lazy: () => import('@/pages/email/EmailSendPage'),
      },
      {
        path: 'email/history',
        lazy: () => import('@/pages/email/EmailHistoryPage'),
      },

      // 템플릿 관리
      {
        path: 'template',
        lazy: () => import('@/pages/template/TemplateListPage'),
      },
      {
        path: 'template/create',
        lazy: () => import('@/pages/template/TemplateCreatePage'),
      },
      {
        path: 'template/:name',
        lazy: () => import('@/pages/template/TemplateEditPage'),
      },

      // 스케줄러
      {
        path: 'scheduler',
        lazy: () => import('@/pages/scheduler/SchedulerListPage'),
      },
      {
        path: 'scheduler/create',
        lazy: () => import('@/pages/scheduler/SchedulerCreatePage'),
      },

      // 온보딩 위저드 (개발 예정)
      {
        path: 'onboarding',
        lazy: () => import('@/pages/onboarding/OnboardingWizardPage'),
      },
      {
        path: 'onboarding/:id',
        lazy: () => import('@/pages/onboarding/OnboardingStatusPage'),
      },

      // Suppression 관리 (개발 예정)
      {
        path: 'suppression',
        lazy: () => import('@/pages/suppression/SuppressionPage'),
      },
    ],
  },
]);
```

### 라우트 요약

| 경로 | 페이지 | 설명 |
|------|--------|------|
| `/` | DashboardPage | 전체 현황 대시보드 |
| `/tenant` | TenantListPage | 테넌트 목록 (ProTable) |
| `/tenant/create` | TenantCreatePage | 테넌트 신규 등록 |
| `/tenant/:id` | TenantDetailPage | 테넌트 상세 및 수정 |
| `/email/send` | EmailSendPage | 즉시 이메일 발송 |
| `/email/history` | EmailHistoryPage | 발송 이력 조회 |
| `/template` | TemplateListPage | SES 템플릿 목록 |
| `/template/create` | TemplateCreatePage | 템플릿 신규 생성 |
| `/template/:name` | TemplateEditPage | 템플릿 편집 |
| `/scheduler` | SchedulerListPage | 예약 발송 작업 목록 |
| `/scheduler/create` | SchedulerCreatePage | 예약 발송 등록 |
| `/onboarding` | OnboardingWizardPage | 신규 테넌트 온보딩 위저드 |
| `/onboarding/:id` | OnboardingStatusPage | 온보딩 진행 상태 |
| `/suppression` | SuppressionPage | Suppression 이메일 관리 |

---

## 7. 환경 설정

### 7.1 환경 변수 파일

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:7092
VITE_APP_TITLE=Joins EMS Admin (Dev)
```

```bash
# .env.production
VITE_API_BASE_URL=/api
VITE_APP_TITLE=Joins EMS Admin
```

### 7.2 Vite 설정

```typescript
// vite.config.ts
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 3000,
      proxy: {
        // 개발 환경에서 /api 경로를 백엔드로 프록시
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:7092',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: mode === 'development',
      rollupOptions: {
        output: {
          // 청크 분리로 초기 로딩 최적화
          manualChunks: {
            vendor: ['react', 'react-dom', 'react-router'],
            antd: ['antd', '@ant-design/pro-components'],
            query: ['@tanstack/react-query'],
          },
        },
      },
    },
  };
});
```

### 7.3 TypeScript 설정

```json
// tsconfig.json (주요 설정)
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

---

## 8. 커스텀 훅 패턴

TanStack Query를 직접 컴포넌트에서 사용하지 않고 커스텀 훅으로 추상화한다.

```typescript
// src/hooks/useTenant.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as tenantApi from '@/api/tenant';

export function useTenantList() {
  return useQuery({
    queryKey: ['tenants'],
    queryFn: tenantApi.list,
  });
}

export function useTenant(tenantId: string) {
  return useQuery({
    queryKey: ['tenants', tenantId],
    queryFn: () => tenantApi.getById(tenantId),
    enabled: !!tenantId,
  });
}

export function useCreateTenant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: tenantApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
}

export function useUpdateTenant(tenantId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateTenantRequest) => tenantApi.update(tenantId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['tenants', tenantId] });
    },
  });
}
```

---

## 9. ProLayout 메뉴 구성

```typescript
// src/layouts/menuConfig.ts
import type { MenuDataItem } from '@ant-design/pro-components';

export const menuItems: MenuDataItem[] = [
  {
    path: '/',
    name: '대시보드',
    icon: 'DashboardOutlined',
  },
  {
    path: '/tenant',
    name: '테넌트 관리',
    icon: 'TeamOutlined',
    children: [
      { path: '/tenant', name: '테넌트 목록' },
      { path: '/tenant/create', name: '테넌트 등록' },
    ],
  },
  {
    path: '/email',
    name: '이메일',
    icon: 'MailOutlined',
    children: [
      { path: '/email/send', name: '이메일 발송' },
      { path: '/email/history', name: '발송 이력' },
    ],
  },
  {
    path: '/template',
    name: '템플릿 관리',
    icon: 'FileTextOutlined',
    children: [
      { path: '/template', name: '템플릿 목록' },
      { path: '/template/create', name: '템플릿 생성' },
    ],
  },
  {
    path: '/scheduler',
    name: '예약 발송',
    icon: 'ClockCircleOutlined',
    children: [
      { path: '/scheduler', name: '작업 목록' },
      { path: '/scheduler/create', name: '예약 등록' },
    ],
  },
  {
    path: '/onboarding',
    name: '온보딩',
    icon: 'RocketOutlined',
  },
  {
    path: '/suppression',
    name: 'Suppression',
    icon: 'StopOutlined',
  },
];
```
