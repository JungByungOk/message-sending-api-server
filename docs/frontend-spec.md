# Joins EMS 프론트엔드 어드민 대시보드 기술 명세서

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
│   │   ├── suppression.ts    # Suppression API 함수
│   │   └── settings.ts       # Settings API 함수
│   ├── components/           # 공용 컴포넌트
│   │   ├── ErrorBoundary.tsx
│   │   ├── PageContainer.tsx
│   │   └── StatusBadge.tsx
│   ├── hooks/                # TanStack Query를 래핑한 커스텀 훅
│   │   ├── useTenant.ts
│   │   ├── useEmail.ts
│   │   ├── useTemplate.ts
│   │   ├── useScheduler.ts
│   │   ├── useOnboarding.ts
│   │   └── useSettings.ts
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
│   │   │   ├── OnboardingWizardPage.tsx  # 도메인 인증 / 이메일 인증 선택
│   │   │   └── OnboardingStatusPage.tsx
│   │   ├── suppression/
│   │   │   └── SuppressionPage.tsx
│   │   └── settings/
│   │       └── SettingsPage.tsx          # API Gateway / Callback / 모드 설정
│   ├── stores/               # Zustand 스토어
│   │   ├── authStore.ts
│   │   └── uiStore.ts
│   ├── types/                # TypeScript 인터페이스 및 타입
│   │   ├── common.ts
│   │   ├── tenant.ts
│   │   ├── email.ts
│   │   ├── scheduler.ts
│   │   ├── onboarding.ts
│   │   ├── suppression.ts
│   │   └── settings.ts
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
  error: string;
  message: string;
}

/** 메시지 태그 */
interface MessageTag {
  name: string;
  value: string;
}
```

---

### 3.2 테넌트 API 타입

```typescript
// src/types/tenant.ts

type VerificationStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
type TenantStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

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
}

interface UpdateTenantRequest {
  tenantName?: string;
  quotaDaily?: number;
  quotaMonthly?: number;
}

interface TenantListResponse {
  totalCount: number;
  tenants: Tenant[];
}

interface QuotaUsage {
  limit: number;
  used: number;
  remaining: number;
}

interface QuotaInfo {
  tenantId: string;
  daily: QuotaUsage;
  monthly: QuotaUsage;
}

/** 발신자 이메일 */
interface TenantSender {
  id: number;
  tenantId: string;
  email: string;
  displayName: string | null;
  isDefault: boolean;
  createdAt: string;
}

interface AddSenderRequest {
  email: string;
  displayName?: string;
  isDefault?: boolean;
}
```

---

### 3.3 SES (이메일 발송) API 타입

```typescript
// src/types/email.ts

/** 일반 이메일 발송 요청 */
interface SendEmailRequest {
  from: string;
  to: string;
  subject: string;
  body: string;
  tags?: MessageTag[];
}

/** 일반 이메일 발송 응답 */
interface SendEmailResponse {
  messageId: string;
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
}

/** SES 이메일 템플릿 */
interface EmailTemplate {
  name: string;
  createdTimestamp?: string;
}

interface CreateTemplateRequest {
  templateName: string;
  subjectPart: string;
  htmlPart: string;
  textPart?: string;
}

interface TemplateResponse {
  awsRequestId: string;
}

type TemplateListResponse = EmailTemplate[];
```

---

### 3.4 스케줄러 API 타입

```typescript
// src/types/scheduler.ts

type JobStatus = 'SCHEDULED' | 'RUNNING' | 'PAUSED' | 'COMPLETE' | 'ERROR' | 'BLOCKED';

interface TemplatedEmailItem {
  id?: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  templateParameters: Record<string, string>;
}

interface ScheduleJobRequest {
  jobName: string;
  jobGroup?: string;
  description?: string;
  startDateAt?: string;           // ISO 8601 형식 (예: "2026-04-10T09:00:00")
  templateName: string;
  from: string;
  templatedEmailList: TemplatedEmailItem[];
  tags?: MessageTag[];
}

interface JobInfo {
  jobName: string;
  groupName: string;
  jobStatus: JobStatus;
  scheduleTime: string;
  lastFiredTime: string | null;
  nextFireTime: string | null;
  description?: string;
}

interface AllJobsResponse {
  numOfAllJobs: number;
  numOfGroups: number;
  numOfRunningJobs: number;
  jobs: JobInfo[];
}

interface JobActionRequest {
  jobName: string;
  jobGroup: string;
}

interface JobActionResponse {
  result: boolean;
  message: string;
}
```

---

### 3.5 온보딩 API 타입

```typescript
// src/types/onboarding.ts

type OnboardingStepStatus = 'COMPLETED' | 'WAITING' | 'PENDING';

interface OnboardingStep {
  step: number;
  name: string;
  status: OnboardingStepStatus;
}

/** 온보딩 시작 요청 (도메인 인증 방식) */
interface OnboardingStartRequest {
  tenantName: string;
  domain: string;
  contactEmail?: string;
}

/** DKIM DNS 레코드 */
interface DkimRecord {
  name: string;
  type: string;
  value: string;
}

/** DKIM 레코드 응답 */
interface DkimRecordsDTO {
  domain: string;
  verificationStatus: 'PENDING' | 'SUCCESS' | 'FAILED';
  dkimRecords: DkimRecord[];
}

/** 온보딩 시작 응답 */
interface OnboardingResultDTO {
  tenant: Tenant;
  dkimRecords: DkimRecordsDTO;
}

/** 온보딩 전체 상태 */
interface OnboardingStatusDTO {
  tenantId: string;
  domain: string;
  steps: OnboardingStep[];
  verificationStatus: 'PENDING' | 'SUCCESS' | 'FAILED';
  tenantStatus: TenantStatus;
}

/** 이메일 인증 상태 */
interface EmailVerificationStatusDTO {
  email: string;
  verificationStatus: 'PENDING' | 'SUCCESS' | 'FAILED';
}

/** 이메일 인증 요청 */
interface VerifyEmailRequest {
  email: string;
}
```

---

### 3.6 Suppression 타입

```typescript
// src/types/suppression.ts

type SuppressionReason = 'BOUNCE' | 'COMPLAINT';

interface SuppressionEntry {
  id: number;
  tenantId: string;
  email: string;
  reason: SuppressionReason;
  createdAt: string;
}

interface SuppressionListResponse {
  totalCount: number;
  suppressions: SuppressionEntry[];
}
```

---

### 3.7 Settings 타입

```typescript
// src/types/settings.ts

type GatewayAuthType = 'API_KEY' | 'IAM';
type DeliveryMode = 'callback' | 'polling';

/** 설정 조회 응답 */
interface AwsSettingsResponse {
  gatewayEndpoint: string;
  gatewayRegion: string;
  gatewayAuthType: GatewayAuthType;
  gatewayApiKeyMasked: string;
  gatewayAccessKey: string;
  gatewaySecretKeyMasked: string;
  gatewaySendPath: string;
  gatewayResultsPath: string;
  gatewayConfigPath: string;
  gatewayConfigured: boolean;
  callbackUrl: string;
  callbackSecretMasked: string;
  callbackConfigured: boolean;
  deliveryMode: DeliveryMode;
  pollingInterval: string;
  updatedAt: string;
}

/** 설정 저장 요청 */
interface SaveAwsSettingsRequest {
  gatewayEndpoint: string;
  gatewayRegion: string;
  gatewayAuthType: GatewayAuthType;
  gatewayApiKey?: string;
  gatewayAccessKey?: string;
  gatewaySecretKey?: string;
  gatewaySendPath?: string;
  gatewayResultsPath?: string;
  gatewayConfigPath?: string;
  gatewayTenantSetupPath?: string;
  callbackUrl?: string;
  callbackSecret?: string;
  deliveryMode: DeliveryMode;
  pollingInterval?: string;
}

/** 연결 테스트 응답 */
interface GatewayTestResponse {
  connected: boolean;
  message: string;
  statusCode: number;
}
```

---

## 4. 상태 관리 전략

### 4.1 TanStack Query (서버 상태)

모든 API 호출은 TanStack Query v5로 관리한다. 서버에서 가져오는 데이터는 컴포넌트 로컬 state가 아닌 Query Cache에 보관한다.

#### queryKey 네이밍 컨벤션

```typescript
['tenants']                          // 테넌트 목록
['tenants', tenantId]                // 특정 테넌트
['tenants', tenantId, 'quota']       // 테넌트 쿼터
['tenants', tenantId, 'senders']     // 테넌트 발신자 목록
['templates']                        // 템플릿 목록
['scheduler', 'jobs']                // 스케줄러 작업 목록
['onboarding', tenantId]             // 온보딩 상태
['onboarding', tenantId, 'dkim']     // DKIM 레코드
['suppression', tenantId]            // Suppression 목록
['settings', 'aws']                  // API Gateway 설정
```

#### 기본 캐시 설정

```typescript
// src/main.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,    // 5분
      gcTime: 1000 * 60 * 10,      // 10분
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});
```

---

### 4.2 Zustand (클라이언트 상태)

#### authStore

```typescript
// src/stores/authStore.ts
interface AuthState {
  apiKey: string | null;
  currentTenantId: string | null;
  currentTenantName: string | null;
  setApiKey: (key: string) => void;
  setCurrentTenant: (id: string, name: string) => void;
  clearAuth: () => void;
}
// localStorage에 'ems-auth' 키로 persist
```

#### uiStore

```typescript
// src/stores/uiStore.ts
interface UiState {
  sidebarCollapsed: boolean;
  theme: 'light' | 'dark';
  notifications: Notification[];
  setSidebarCollapsed: (collapsed: boolean) => void;
  toggleSidebar: () => void;
  setTheme: (theme: Theme) => void;
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  removeNotification: (id: string) => void;
}
```

---

## 5. API 클라이언트 설정

```typescript
// src/api/client.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// 요청 인터셉터: Authorization 헤더에 API Key 자동 주입
apiClient.interceptors.request.use((config) => {
  const { apiKey } = useAuthStore.getState();
  if (apiKey) {
    config.headers['Authorization'] = apiKey;
  }
  return config;
});

// 응답 인터셉터: 401 시 인증 정보 초기화
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().clearAuth();
    }
    return Promise.reject(error);
  }
);
```

---

## 6. 라우팅 구조

React Router v7의 `createBrowserRouter`를 사용한다.

### 라우트 요약

| 경로 | 페이지 | 설명 |
|------|--------|------|
| `/` | DashboardPage | 전체 현황 대시보드 |
| `/tenant` | TenantListPage | 테넌트 목록 (ProTable) |
| `/tenant/create` | TenantCreatePage | 테넌트 신규 등록 |
| `/tenant/:id` | TenantDetailPage | 테넌트 상세 및 수정, 발신자 이메일 관리 |
| `/email/send` | EmailSendPage | 즉시 이메일 발송 |
| `/email/history` | EmailHistoryPage | 발송 이력 조회 |
| `/template` | TemplateListPage | SES 템플릿 목록 |
| `/template/create` | TemplateCreatePage | 템플릿 신규 생성 |
| `/template/:name` | TemplateEditPage | 템플릿 편집 |
| `/scheduler` | SchedulerListPage | 예약 발송 작업 목록 |
| `/scheduler/create` | SchedulerCreatePage | 예약 발송 등록 |
| `/onboarding` | OnboardingWizardPage | 신규 테넌트 온보딩 (도메인/이메일 인증 선택) |
| `/onboarding/:id` | OnboardingStatusPage | 온보딩 진행 상태 |
| `/suppression` | SuppressionPage | Suppression 이메일 관리 |
| `/settings` | SettingsPage | API Gateway, Callback, 수신 모드 설정 |

---

## 7. 페이지별 주요 기능

### 7.1 OnboardingWizardPage

온보딩 시 인증 방식 선택 UI를 제공합니다.

```
Step 1: 테넌트 기본 정보 입력 (이름, 도메인)
Step 2: 인증 방식 선택
  ├── 도메인 인증 (DKIM): DNS CNAME 레코드 추가 → DKIM 상태 폴링
  └── 이메일 개별 인증: 이메일 주소 입력 → 인증 메일 수신 확인
Step 3: ConfigSet 구성 + 테넌트 활성화
```

- DKIM 인증 방식: `GET /onboarding/{id}/dkim` 폴링으로 `verificationStatus === 'SUCCESS'` 대기
- 이메일 인증 방식: `GET /onboarding/{id}/email-status/{email}` 폴링

### 7.2 SettingsPage

API Gateway 연결 설정과 발송 결과 수신 모드를 관리합니다.

```
섹션 1: API Gateway 연결
  - Endpoint URL, 리전, 인증 방식 (API_KEY / IAM), API Key
  - 경로 설정 (/send-email, /results, /config, /tenant-setup)
  - 연결 테스트 버튼 (POST /settings/aws/test)

섹션 2: Callback 설정
  - Callback URL (Lambda가 ESM을 호출할 주소)
  - Callback Secret (X-Callback-Secret 헤더 검증용)

섹션 3: 수신 모드
  - callback: 실시간 콜백 + 보정 폴링
  - polling: 보정 폴링만
  - 폴링 주기 (ms)

저장 시 ESM DB + SSM Parameter Store 자동 동기화
```

### 7.3 TenantDetailPage

테넌트 상세 정보와 발신자 이메일 목록을 함께 관리합니다.

- 테넌트 기본 정보 수정
- 할당량(일/월) 수정
- API Key 재발급
- 발신자 이메일 목록 조회/추가/삭제 (도메인 제한 안내 포함)

---

## 8. 환경 설정

### 8.1 환경 변수 파일

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

### 8.2 Vite 설정

```typescript
// vite.config.ts
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:7092',
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
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router'],
          antd: ['antd', '@ant-design/pro-components'],
          query: ['@tanstack/react-query'],
        },
      },
    },
  },
}));
```

### 8.3 TypeScript 설정

```json
// tsconfig.json (주요 설정)
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "paths": { "@/*": ["./src/*"] }
  }
}
```

---

## 9. ProLayout 메뉴 구성

```typescript
// src/layouts/menuConfig.ts
export const menuItems: MenuDataItem[] = [
  { path: '/', name: '대시보드', icon: 'DashboardOutlined' },
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
  { path: '/onboarding', name: '온보딩', icon: 'RocketOutlined' },
  { path: '/suppression', name: 'Suppression', icon: 'StopOutlined' },
  { path: '/settings', name: '설정', icon: 'SettingOutlined' },
];
```

---

## 10. 커스텀 훅 패턴

TanStack Query를 직접 컴포넌트에서 사용하지 않고 커스텀 훅으로 추상화한다.

```typescript
// src/hooks/useSettings.ts
export function useAwsSettings() {
  return useQuery({
    queryKey: ['settings', 'aws'],
    queryFn: settingsApi.getAwsSettings,
  });
}

export function useSaveAwsSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: settingsApi.saveAwsSettings,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settings', 'aws'] });
    },
  });
}

export function useTestGateway() {
  return useMutation({
    mutationFn: settingsApi.testGateway,
  });
}
```
