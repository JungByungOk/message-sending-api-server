# Joins EMS 프론트엔드 개발 계획서

> **플랫폼**: Multi-tenant SaaS Email Management System
> **목적**: AWS SES 기반 이메일 발송 관리 어드민 대시보드
> **작성일**: 2026-04-04

---

## 목차

1. [기술 스택](#1-기술-스택)
2. [디렉토리 구조](#2-디렉토리-구조)
3. [공통 기반 구조](#3-공통-기반-구조)
4. [Phase 1: 기본 레이아웃 + 테넌트 관리](#4-phase-1-기본-레이아웃--테넌트-관리)
5. [Phase 2: 이메일 발송 + 템플릿 관리](#5-phase-2-이메일-발송--템플릿-관리)
6. [Phase 3: 스케줄러 관리](#6-phase-3-스케줄러-관리)
7. [Phase 4: 온보딩 + 도메인 인증](#7-phase-4-온보딩--도메인-인증)
8. [Phase 5: 대시보드 + 모니터링](#8-phase-5-대시보드--모니터링)
9. [TypeScript 타입 정의](#9-typescript-타입-정의)
10. [개발 환경 설정](#10-개발-환경-설정)
11. [Phase별 일정 요약](#11-phase별-일정-요약)

---

## 1. 기술 스택

| 분류 | 라이브러리 | 버전 | 용도 |
|------|-----------|------|------|
| 프레임워크 | React | 19 | UI 렌더링 |
| 언어 | TypeScript | 5.9 | 타입 안전성 |
| 빌드 도구 | Vite | 7 | 개발 서버, 번들링 |
| UI 컴포넌트 | Ant Design | 5 | 기본 UI 컴포넌트 |
| Pro 컴포넌트 | @ant-design/pro-components | 2.8 | ProLayout, ProTable, ProForm |
| 서버 상태 | TanStack Query | 5 | API 캐싱, 비동기 상태 |
| 클라이언트 상태 | Zustand | 5 | 전역 UI 상태 |
| 라우팅 | React Router | 7 | SPA 라우팅 |
| HTTP 클라이언트 | Axios | 1.13 | API 통신 |

---

## 2. 디렉토리 구조

```
frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                        # Axios 클라이언트 + API 함수
│   │   ├── client.ts               # Axios 인스턴스 + 인터셉터
│   │   ├── tenant.ts               # 테넌트 API
│   │   ├── email.ts                # 이메일 발송 API
│   │   ├── template.ts             # 템플릿 API
│   │   ├── scheduler.ts            # 스케줄러 API
│   │   ├── onboarding.ts           # 온보딩 API
│   │   ├── identity.ts             # SES Identity API
│   │   └── suppression.ts         # Suppression API
│   ├── components/                 # 공통 재사용 컴포넌트
│   │   ├── ApiKeyDisplay/          # API Key 마스킹 표시 컴포넌트
│   │   ├── StatusBadge/            # 상태 뱃지
│   │   ├── CopyButton/             # 클립보드 복사 버튼
│   │   ├── ConfirmModal/           # 삭제/위험 확인 모달
│   │   └── PageContainer/         # 페이지 래퍼 컨테이너
│   ├── hooks/                      # Custom hooks (TanStack Query 래퍼)
│   │   ├── useTenants.ts
│   │   ├── useEmail.ts
│   │   ├── useTemplates.ts
│   │   ├── useScheduler.ts
│   │   └── useOnboarding.ts
│   ├── layouts/                    # ProLayout 기반 레이아웃
│   │   ├── AdminLayout.tsx         # 메인 레이아웃
│   │   ├── menuConfig.ts           # 사이드바 메뉴 설정
│   │   └── breadcrumbConfig.ts    # 브레드크럼 설정
│   ├── pages/                      # 페이지 컴포넌트
│   │   ├── dashboard/
│   │   │   └── DashboardPage.tsx
│   │   ├── tenant/
│   │   │   ├── TenantListPage.tsx
│   │   │   ├── TenantDetailPage.tsx
│   │   │   └── TenantFormModal.tsx
│   │   ├── email/
│   │   │   ├── EmailSendPage.tsx
│   │   │   └── EmailHistoryPage.tsx
│   │   ├── template/
│   │   │   ├── TemplateListPage.tsx
│   │   │   └── TemplateEditorPage.tsx
│   │   ├── scheduler/
│   │   │   └── SchedulerPage.tsx
│   │   ├── onboarding/
│   │   │   └── OnboardingWizardPage.tsx
│   │   └── suppression/
│   │       └── SuppressionPage.tsx
│   ├── stores/                     # Zustand stores
│   │   ├── authStore.ts            # 인증 (API Key) 상태
│   │   └── uiStore.ts              # UI 상태 (사이드바 등)
│   ├── types/                      # TypeScript 타입 정의
│   │   ├── tenant.ts
│   │   ├── email.ts
│   │   ├── template.ts
│   │   ├── scheduler.ts
│   │   ├── onboarding.ts
│   │   └── common.ts
│   ├── utils/                      # 유틸리티 함수
│   │   ├── format.ts               # 날짜/숫자 포맷
│   │   ├── constants.ts            # 상수 정의
│   │   └── queryClient.ts         # TanStack Query 클라이언트
│   ├── App.tsx
│   ├── main.tsx
│   └── router.tsx                  # React Router 라우트 정의
├── package.json
├── vite.config.ts
└── tsconfig.json
```

---

## 3. 공통 기반 구조

### 3.1 Axios 클라이언트 설정

```typescript
// src/api/client.ts
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:7092',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// 요청 인터셉터: API Key 자동 주입
apiClient.interceptors.request.use((config) => {
  const apiKey = useAuthStore.getState().apiKey;
  if (apiKey) {
    config.headers['Authorization'] = apiKey;
  }
  return config;
});

// 응답 인터셉터: 공통 에러 처리
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      useAuthStore.getState().clearApiKey();
      // 로그인 페이지로 이동 (추후 구현)
    }
    return Promise.reject(error);
  },
);
```

### 3.2 TanStack Query 설정

```typescript
// src/utils/queryClient.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,     // 5분 캐시
      retry: 2,
      refetchOnWindowFocus: false,
    },
    mutations: {
      onError: (error) => {
        // Ant Design notification으로 에러 표시
        console.error('Mutation error:', error);
      },
    },
  },
});
```

### 3.3 Zustand Store 설계

```typescript
// src/stores/authStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  apiKey: string | null;
  selectedTenantId: string | null;
  setApiKey: (key: string) => void;
  clearApiKey: () => void;
  setSelectedTenant: (id: string) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      apiKey: null,
      selectedTenantId: null,
      setApiKey: (key) => set({ apiKey: key }),
      clearApiKey: () => set({ apiKey: null, selectedTenantId: null }),
      setSelectedTenant: (id) => set({ selectedTenantId: id }),
    }),
    { name: 'ems-auth' },
  ),
);

// src/stores/uiStore.ts
import { create } from 'zustand';

interface UiState {
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
}

export const useUiStore = create<UiState>((set) => ({
  sidebarCollapsed: false,
  toggleSidebar: () =>
    set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
}));
```

### 3.4 ProLayout 기반 레이아웃

```typescript
// src/layouts/menuConfig.ts
import {
  DashboardOutlined,
  TeamOutlined,
  MailOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  GlobalOutlined,
  StopOutlined,
} from '@ant-design/icons';

export const menuItems = [
  { path: '/dashboard', name: '대시보드', icon: <DashboardOutlined /> },
  { path: '/tenant',    name: '테넌트 관리', icon: <TeamOutlined /> },
  {
    path: '/email',
    name: '이메일',
    icon: <MailOutlined />,
    children: [
      { path: '/email/send',    name: '이메일 발송' },
      { path: '/email/history', name: '발송 이력' },
    ],
  },
  { path: '/template',   name: '템플릿 관리', icon: <FileTextOutlined /> },
  { path: '/scheduler',  name: '스케줄러',    icon: <ClockCircleOutlined /> },
  { path: '/onboarding', name: '온보딩',      icon: <GlobalOutlined /> },
  { path: '/suppression',name: 'Suppression', icon: <StopOutlined /> },
];
```

### 3.5 React Router 7 라우트 구조

```typescript
// src/router.tsx
import { createBrowserRouter } from 'react-router-dom';
import AdminLayout from './layouts/AdminLayout';
import DashboardPage       from './pages/dashboard/DashboardPage';
import TenantListPage      from './pages/tenant/TenantListPage';
import TenantDetailPage    from './pages/tenant/TenantDetailPage';
import EmailSendPage       from './pages/email/EmailSendPage';
import EmailHistoryPage    from './pages/email/EmailHistoryPage';
import TemplateListPage    from './pages/template/TemplateListPage';
import TemplateEditorPage  from './pages/template/TemplateEditorPage';
import SchedulerPage       from './pages/scheduler/SchedulerPage';
import OnboardingWizardPage from './pages/onboarding/OnboardingWizardPage';
import SuppressionPage     from './pages/suppression/SuppressionPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AdminLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'dashboard',         element: <DashboardPage /> },
      { path: 'tenant',            element: <TenantListPage /> },
      { path: 'tenant/:id',        element: <TenantDetailPage /> },
      { path: 'email/send',        element: <EmailSendPage /> },
      { path: 'email/history',     element: <EmailHistoryPage /> },
      { path: 'template',          element: <TemplateListPage /> },
      { path: 'template/:name',    element: <TemplateEditorPage /> },
      { path: 'scheduler',         element: <SchedulerPage /> },
      { path: 'onboarding',        element: <OnboardingWizardPage /> },
      { path: 'suppression',       element: <SuppressionPage /> },
    ],
  },
]);
```

---

## 4. Phase 1: 기본 레이아웃 + 테넌트 관리

> **연동 백엔드**: Backend Phase 1 (Tenant API)
> **목표**: 프로젝트 초기화, 공통 기반, 테넌트 CRUD

### 4.1 작업 범위

| 작업 | 설명 |
|------|------|
| 프로젝트 초기화 | Vite + React 19 + TypeScript 프로젝트 생성 |
| 공통 기반 구축 | Axios 클라이언트, QueryClient, Zustand 스토어 |
| ProLayout 설정 | 사이드바, 헤더, 브레드크럼 |
| API Key 입력 UI | 최초 진입 시 API Key 입력 모달 |
| 테넌트 목록 | ProTable 기반 목록 (페이지네이션, 검색) |
| 테넌트 생성/수정 | ProForm 기반 모달 폼 |
| 테넌트 상세 | 상세 정보 + API Key 재생성 |
| 테넌트 삭제 | 확인 다이얼로그 |

### 4.2 페이지 및 라우트

| 페이지 | 경로 | 컴포넌트 |
|--------|------|---------|
| 테넌트 목록 | `/tenant` | `TenantListPage` |
| 테넌트 상세 | `/tenant/:id` | `TenantDetailPage` |

### 4.3 API 연동

```typescript
// src/api/tenant.ts
import { apiClient } from './client';
import type { Tenant, TenantCreateRequest, TenantUpdateRequest, PageResponse } from '../types/tenant';

export const tenantApi = {
  list: (params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<Tenant>>('/tenant/list', { params }),

  getById: (id: string) =>
    apiClient.get<Tenant>(`/tenant/${id}`),

  create: (data: TenantCreateRequest) =>
    apiClient.post<Tenant>('/tenant', data),

  update: (id: string, data: TenantUpdateRequest) =>
    apiClient.patch<Tenant>(`/tenant/${id}`, data),

  delete: (id: string) =>
    apiClient.delete(`/tenant/${id}`),

  regenerateKey: (id: string) =>
    apiClient.post<{ apiKey: string }>(`/tenant/${id}/regenerate-key`),
};
```

### 4.4 커스텀 훅

```typescript
// src/hooks/useTenants.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tenantApi } from '../api/tenant';

export const TENANT_KEYS = {
  all: ['tenants'] as const,
  list: (params?: object) => [...TENANT_KEYS.all, 'list', params] as const,
  detail: (id: string) => [...TENANT_KEYS.all, 'detail', id] as const,
};

export function useTenantList(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: TENANT_KEYS.list(params),
    queryFn: () => tenantApi.list(params).then((r) => r.data),
  });
}

export function useTenantDetail(id: string) {
  return useQuery({
    queryKey: TENANT_KEYS.detail(id),
    queryFn: () => tenantApi.getById(id).then((r) => r.data),
    enabled: !!id,
  });
}

export function useCreateTenant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: tenantApi.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: TENANT_KEYS.all }),
  });
}

export function useDeleteTenant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: tenantApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: TENANT_KEYS.all }),
  });
}

export function useRegenerateApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: tenantApi.regenerateKey,
    onSuccess: (_data, id) =>
      qc.invalidateQueries({ queryKey: TENANT_KEYS.detail(id) }),
  });
}
```

### 4.5 주요 컴포넌트 설계

**TenantListPage - ProTable 컬럼 설계**

| 컬럼 | 필드 | 타입 | 기능 |
|------|------|------|------|
| 테넌트명 | `name` | string | 정렬, 검색 |
| 도메인 | `domain` | string | 검색 |
| 상태 | `status` | enum | 필터 (ACTIVE/INACTIVE) |
| API Key | `apiKey` | string | 마스킹 표시 + 복사 버튼 |
| 생성일 | `createdAt` | datetime | 정렬 |
| 액션 | - | - | 상세/수정/삭제 버튼 |

**상태 관리 전략**

```
서버 상태 (TanStack Query)
├── 테넌트 목록 캐시 (5분 staleTime)
├── 테넌트 상세 캐시
└── Mutation: create / update / delete / regenerateKey

클라이언트 상태 (Zustand)
├── authStore.apiKey         ← 현재 사용 API Key
└── authStore.selectedTenantId ← 선택된 테넌트
```

---

## 5. Phase 2: 이메일 발송 + 템플릿 관리

> **연동 백엔드**: Backend 기존 SES API
> **목표**: 이메일 직접 발송, 템플릿 CRUD, HTML 미리보기

### 5.1 작업 범위

| 작업 | 설명 |
|------|------|
| 텍스트 이메일 발송 | 수신자, 제목, 본문 입력 폼 |
| 템플릿 이메일 발송 | 템플릿 선택 + 데이터 변수 입력 |
| 템플릿 목록 | ProTable 기반 목록 |
| 템플릿 생성/수정 | HTML 에디터 + 미리보기 |
| 템플릿 삭제 | 확인 다이얼로그 |
| 발송 이력 | 발송 내역 조회 (추후 확장) |

### 5.2 페이지 및 라우트

| 페이지 | 경로 | 컴포넌트 |
|--------|------|---------|
| 이메일 발송 | `/email/send` | `EmailSendPage` |
| 발송 이력 | `/email/history` | `EmailHistoryPage` |
| 템플릿 목록 | `/template` | `TemplateListPage` |
| 템플릿 편집기 | `/template/:name` | `TemplateEditorPage` |

### 5.3 API 연동

```typescript
// src/api/email.ts
import { apiClient } from './client';
import type { TextMailRequest, TemplatedMailRequest } from '../types/email';

export const emailApi = {
  sendText: (data: TextMailRequest) =>
    apiClient.post('/ses/text-mail', data),

  sendTemplated: (data: TemplatedMailRequest) =>
    apiClient.post('/ses/templated-mail', data),
};

// src/api/template.ts
import { apiClient } from './client';
import type { Template, TemplateCreateRequest } from '../types/template';

export const templateApi = {
  list: () =>
    apiClient.get<Template[]>('/ses/templates'),

  create: (data: TemplateCreateRequest) =>
    apiClient.post<Template>('/ses/template', data),

  update: (name: string, data: Partial<TemplateCreateRequest>) =>
    apiClient.patch<Template>(`/ses/template`, { ...data, templateName: name }),

  delete: (name: string) =>
    apiClient.delete(`/ses/template`, { params: { templateName: name } }),
};
```

### 5.4 이메일 발송 폼 설계

```
EmailSendPage
├── Tabs: [텍스트 발송] [템플릿 발송]
│
├── 텍스트 발송 탭
│   ├── ProForm
│   │   ├── ProFormText       수신자 이메일 (복수 입력 지원)
│   │   ├── ProFormText       발신자 이메일
│   │   ├── ProFormText       제목
│   │   └── ProFormTextArea   본문 (HTML/텍스트)
│   └── 발송 버튼 → useMutation(emailApi.sendText)
│
└── 템플릿 발송 탭
    ├── ProForm
    │   ├── ProFormSelect     템플릿 선택 (useTemplateList로 populate)
    │   ├── ProFormText       수신자 이메일
    │   └── ProFormTextArea   템플릿 데이터 (JSON 입력)
    └── 발송 버튼 → useMutation(emailApi.sendTemplated)
```

### 5.5 템플릿 편집기 설계

```
TemplateEditorPage
├── ProForm (상단)
│   ├── ProFormText     템플릿명
│   ├── ProFormText     제목 (Subject)
│   └── ProFormText     텍스트 파트 (선택)
│
├── HTML 편집기 (중앙) - textarea 또는 Monaco Editor
│
├── 미리보기 패널 (우측)
│   └── iframe으로 HTML 실시간 렌더링
│
└── 저장/취소 버튼
```

### 5.6 상태 관리 전략

```
서버 상태 (TanStack Query)
├── 템플릿 목록 캐시
└── Mutation: create / update / delete / sendText / sendTemplated

클라이언트 상태 (컴포넌트 로컬)
├── 발송 폼 탭 선택 상태
└── 템플릿 HTML 편집 내용 (저장 전)
```

---

## 6. Phase 3: 스케줄러 관리

> **연동 백엔드**: Backend 기존 Scheduler API
> **목표**: 스케줄링 작업 목록 조회, 상태 제어

### 6.1 작업 범위

| 작업 | 설명 |
|------|------|
| 작업 목록 | ProTable 기반 스케줄러 작업 목록 |
| 작업 생성 | Cron 표현식 기반 작업 등록 |
| 작업 일시정지 | 개별 작업 일시정지 (pause) |
| 작업 재개 | 개별 작업 재개 (resume) |
| 작업 중지 | 개별 작업 완전 중지 (stop) |
| 작업 삭제 | 개별 또는 전체 삭제 |
| 상태 표시 | RUNNING / PAUSED / STOPPED 뱃지 |

### 6.2 페이지 및 라우트

| 페이지 | 경로 | 컴포넌트 |
|--------|------|---------|
| 스케줄러 관리 | `/scheduler` | `SchedulerPage` |

### 6.3 API 연동

```typescript
// src/api/scheduler.ts
import { apiClient } from './client';
import type { SchedulerJob, JobCreateRequest } from '../types/scheduler';

export const schedulerApi = {
  list: () =>
    apiClient.get<SchedulerJob[]>('/scheduler/jobs'),

  create: (data: JobCreateRequest) =>
    apiClient.post<SchedulerJob>('/scheduler/job', data),

  pause: (jobId: string) =>
    apiClient.put(`/scheduler/job/pause`, null, { params: { jobId } }),

  resume: (jobId: string) =>
    apiClient.put(`/scheduler/job/resume`, null, { params: { jobId } }),

  stop: (jobId: string) =>
    apiClient.put(`/scheduler/job/stop`, null, { params: { jobId } }),

  delete: (jobId: string) =>
    apiClient.delete(`/scheduler/job`, { params: { jobId } }),

  deleteAll: () =>
    apiClient.delete(`/scheduler/job/all`),
};
```

### 6.4 스케줄러 페이지 설계

```
SchedulerPage
├── 상단 툴바
│   ├── [작업 생성] 버튼
│   └── [전체 삭제] 버튼 (위험 확인 모달)
│
├── ProTable
│   ├── 컬럼: 작업ID / 작업명 / Cron 표현식 / 상태 / 마지막 실행 / 다음 실행
│   └── 액션 컬럼
│       ├── 일시정지 버튼 (RUNNING 상태일 때만 활성)
│       ├── 재개 버튼 (PAUSED 상태일 때만 활성)
│       ├── 중지 버튼 (RUNNING/PAUSED 상태일 때만 활성)
│       └── 삭제 버튼
│
└── 작업 생성 모달 (ProForm)
    ├── ProFormText    작업명
    ├── ProFormText    Cron 표현식 (도움말 링크 포함)
    └── ProFormSelect  대상 이메일 작업 타입
```

### 6.5 상태 뱃지 컴포넌트

```typescript
// src/components/StatusBadge/index.tsx
import { Badge } from 'antd';

const STATUS_MAP = {
  RUNNING: { status: 'processing', text: '실행 중' },
  PAUSED:  { status: 'warning',    text: '일시정지' },
  STOPPED: { status: 'error',      text: '중지됨' },
} as const;

export function StatusBadge({ status }: { status: keyof typeof STATUS_MAP }) {
  const { status: badgeStatus, text } = STATUS_MAP[status];
  return <Badge status={badgeStatus} text={text} />;
}
```

---

## 7. Phase 4: 온보딩 + 도메인 인증

> **연동 백엔드**: Backend Phase 2 (SES Identity), Phase 4 (Onboarding)
> **목표**: 신규 테넌트 도메인 등록 → DKIM DNS 설정 → 인증 확인 → 활성화

### 7.1 작업 범위

| 작업 | 설명 |
|------|------|
| 온보딩 위저드 | 4단계 Step 위저드 UI |
| 도메인 입력 | 테넌트 선택 + 도메인 입력 |
| DKIM 안내 | DNS 레코드 표시 (복사 버튼 포함) |
| 인증 상태 확인 | DNS 전파 상태 폴링 |
| 도메인 활성화 | 최종 활성화 버튼 |

### 7.2 페이지 및 라우트

| 페이지 | 경로 | 컴포넌트 |
|--------|------|---------|
| 온보딩 위저드 | `/onboarding` | `OnboardingWizardPage` |

### 7.3 API 연동

```typescript
// src/api/onboarding.ts
import { apiClient } from './client';
import type {
  OnboardingStartRequest,
  OnboardingStatus,
  DkimRecords,
} from '../types/onboarding';

export const onboardingApi = {
  start: (data: OnboardingStartRequest) =>
    apiClient.post<{ onboardingId: string }>('/onboarding/start', data),

  getStatus: (id: string) =>
    apiClient.get<OnboardingStatus>(`/onboarding/${id}/status`),

  getDkim: (id: string) =>
    apiClient.get<DkimRecords>(`/onboarding/${id}/dkim`),

  activate: (id: string) =>
    apiClient.post(`/onboarding/${id}/activate`),
};

// src/api/identity.ts
import { apiClient } from './client';
import type { SesIdentity } from '../types/onboarding';

export const identityApi = {
  register: (domain: string) =>
    apiClient.post<SesIdentity>('/ses/identity', { domain }),

  getByDomain: (domain: string) =>
    apiClient.get<SesIdentity>(`/ses/identity/${domain}`),

  delete: (domain: string) =>
    apiClient.delete(`/ses/identity/${domain}`),
};
```

### 7.4 온보딩 위저드 설계

```
OnboardingWizardPage
│
├── Steps 컴포넌트 (4단계)
│   ├── Step 1: 도메인 등록
│   ├── Step 2: DKIM DNS 설정
│   ├── Step 3: 인증 확인
│   └── Step 4: 활성화 완료
│
├── Step 1 - 도메인 등록
│   ├── ProFormSelect  테넌트 선택
│   ├── ProFormText    도메인 입력 (예: example.com)
│   └── [다음] 버튼 → onboardingApi.start()
│
├── Step 2 - DKIM DNS 설정
│   ├── Alert: "아래 DNS 레코드를 도메인 설정에 추가하세요"
│   ├── Table: DKIM 레코드 목록 (Type / Name / Value)
│   │   └── 각 행에 복사 버튼
│   └── [인증 확인으로 이동] 버튼
│
├── Step 3 - 인증 확인
│   ├── 상태 표시: DNS 전파 대기 중... (Spin)
│   ├── [인증 상태 확인] 버튼 → onboardingApi.getStatus() 폴링
│   │   ├── PENDING  → 대기 중 안내
│   │   ├── VERIFIED → Step 4로 자동 이동
│   │   └── FAILED   → 오류 메시지 + 재시도
│   └── 자동 폴링: 30초 간격 (useQuery refetchInterval)
│
└── Step 4 - 활성화 완료
    ├── Result 컴포넌트 (성공 아이콘)
    ├── 활성화 정보 요약
    └── [테넌트 관리로 이동] 버튼
```

### 7.5 DKIM 폴링 전략

```typescript
// Step 3에서 사용하는 상태 폴링 훅
export function useOnboardingStatus(id: string, enabled: boolean) {
  return useQuery({
    queryKey: ['onboarding', 'status', id],
    queryFn: () => onboardingApi.getStatus(id).then((r) => r.data),
    enabled: !!id && enabled,
    refetchInterval: (data) => {
      // VERIFIED 또는 FAILED 상태면 폴링 중지
      if (data?.status === 'VERIFIED' || data?.status === 'FAILED') {
        return false;
      }
      return 30_000; // 30초 간격 폴링
    },
  });
}
```

---

## 8. Phase 5: 대시보드 + 모니터링

> **연동 백엔드**: Backend Phase 3 (Quota), Phase 5 (Suppression, Callback)
> **목표**: 발송 현황 시각화, 쿼터 관리, Suppression 목록 관리

### 8.1 작업 범위

| 작업 | 설명 |
|------|------|
| 대시보드 | 발송 현황 차트, 테넌트 요약 카드 |
| 쿼터 관리 | 테넌트별 발송 쿼터 조회/수정 |
| Suppression 목록 | 발송 차단 이메일 목록 조회/삭제 |
| 이벤트 모니터링 | SES 이벤트 콜백 상태 조회 |

### 8.2 페이지 및 라우트

| 페이지 | 경로 | 컴포넌트 |
|--------|------|---------|
| 대시보드 | `/dashboard` | `DashboardPage` |
| Suppression 관리 | `/suppression` | `SuppressionPage` |

### 8.3 API 연동

```typescript
// src/api/suppression.ts
import { apiClient } from './client';
import type { SuppressionEntry } from '../types/email';

export const suppressionApi = {
  list: (tenantId: string) =>
    apiClient.get<SuppressionEntry[]>(`/tenant/${tenantId}/suppression`),

  delete: (tenantId: string, email: string) =>
    apiClient.delete(`/tenant/${tenantId}/suppression/${encodeURIComponent(email)}`),
};

// 쿼터 API (향후 추가)
export const quotaApi = {
  get: (tenantId: string) =>
    apiClient.get(`/tenant/${tenantId}/quota`),

  update: (tenantId: string, data: { dailyLimit: number; monthlyLimit: number }) =>
    apiClient.patch(`/tenant/${tenantId}/quota`, data),
};
```

### 8.4 대시보드 설계

```
DashboardPage
│
├── 요약 카드 행 (4개)
│   ├── 총 테넌트 수
│   ├── 오늘 발송 건수
│   ├── 이번 달 발송 건수
│   └── 실패율 (%)
│
├── 발송 현황 차트 (Ant Design Charts 또는 Recharts)
│   ├── 일별 발송량 Line Chart (최근 30일)
│   └── 테넌트별 발송량 Bar Chart
│
├── 스케줄러 현황 (작은 테이블)
│   └── 실행 중인 작업 목록 (최대 5개)
│
└── 최근 이벤트 (타임라인)
    └── SES 콜백 이벤트 최근 10건
```

### 8.5 Suppression 페이지 설계

```
SuppressionPage
├── 테넌트 선택 (ProFormSelect)
│
└── ProTable
    ├── 컬럼: 이메일 / 차단 사유 / 차단 일시
    ├── 검색: 이메일 주소 검색
    └── 액션: 차단 해제 버튼 (삭제 확인 포함)
```

---

## 9. TypeScript 타입 정의

```typescript
// src/types/common.ts
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ApiError {
  code: string;
  message: string;
  timestamp: string;
}

// src/types/tenant.ts
export type TenantStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface Tenant {
  id: string;
  name: string;
  domain: string;
  status: TenantStatus;
  apiKey: string;
  createdAt: string;
  updatedAt: string;
}

export interface TenantCreateRequest {
  name: string;
  domain: string;
}

export interface TenantUpdateRequest {
  name?: string;
  status?: TenantStatus;
}

// src/types/email.ts
export interface TextMailRequest {
  to: string[];
  from: string;
  subject: string;
  body: string;
}

export interface TemplatedMailRequest {
  to: string[];
  from: string;
  templateName: string;
  templateData: Record<string, string>;
}

export interface SuppressionEntry {
  email: string;
  reason: string;
  createdAt: string;
}

// src/types/template.ts
export interface Template {
  templateName: string;
  subjectPart: string;
  htmlPart: string;
  textPart?: string;
  createdAt: string;
}

export interface TemplateCreateRequest {
  templateName: string;
  subjectPart: string;
  htmlPart: string;
  textPart?: string;
}

// src/types/scheduler.ts
export type JobStatus = 'RUNNING' | 'PAUSED' | 'STOPPED';

export interface SchedulerJob {
  jobId: string;
  jobName: string;
  cronExpression: string;
  status: JobStatus;
  lastExecutedAt?: string;
  nextExecutionAt?: string;
}

export interface JobCreateRequest {
  jobName: string;
  cronExpression: string;
  jobType: string;
}

// src/types/onboarding.ts
export type OnboardingStatusType = 'PENDING' | 'VERIFIED' | 'FAILED';

export interface OnboardingStartRequest {
  tenantId: string;
  domain: string;
}

export interface OnboardingStatus {
  onboardingId: string;
  status: OnboardingStatusType;
  domain: string;
  message?: string;
}

export interface DkimRecord {
  type: string;
  name: string;
  value: string;
}

export interface DkimRecords {
  records: DkimRecord[];
}

export interface SesIdentity {
  domain: string;
  verificationStatus: string;
  dkimStatus: string;
}
```

---

## 10. 개발 환경 설정

### 10.1 package.json

```json
{
  "name": "joins-ems-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext ts,tsx"
  },
  "dependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router-dom": "^7.0.0",
    "antd": "^5.0.0",
    "@ant-design/icons": "^5.0.0",
    "@ant-design/pro-components": "^2.8.0",
    "@tanstack/react-query": "^5.0.0",
    "zustand": "^5.0.0",
    "axios": "^1.13.0"
  },
  "devDependencies": {
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "@vitejs/plugin-react": "^4.0.0",
    "typescript": "^5.9.0",
    "vite": "^7.0.0"
  }
}
```

### 10.2 vite.config.ts

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/ses':        { target: 'http://localhost:7092', changeOrigin: true },
      '/tenant':     { target: 'http://localhost:7092', changeOrigin: true },
      '/scheduler':  { target: 'http://localhost:7092', changeOrigin: true },
      '/onboarding': { target: 'http://localhost:7092', changeOrigin: true },
    },
  },
});
```

### 10.3 환경 변수 (.env)

```bash
VITE_API_BASE_URL=http://localhost:7092
VITE_APP_TITLE=Joins EMS Admin
```

---

## 11. Phase별 일정 요약

```
Phase 1 ──────────────────────────────────────────────────────
  Week 1-2: 프로젝트 초기화, 공통 기반, ProLayout
  Week 3-4: 테넌트 CRUD (목록 / 생성 / 수정 / 삭제 / 상세)

Phase 2 ──────────────────────────────────────────────────────
  Week 5-6: 이메일 발송 폼 (텍스트 / 템플릿)
  Week 7-8: 템플릿 CRUD + HTML 미리보기

Phase 3 ──────────────────────────────────────────────────────
  Week 9-10: 스케줄러 목록 + 상태 제어 (pause / resume / stop)

Phase 4 ──────────────────────────────────────────────────────
  Week 11-12: 온보딩 위저드 (도메인 등록 → DKIM → 인증 → 활성화)

Phase 5 ──────────────────────────────────────────────────────
  Week 13-14: 대시보드 차트 + 요약 카드
  Week 15-16: Suppression 관리 + 쿼터 관리
```

### Phase별 의존성 매트릭스

| Frontend Phase | 의존 Backend | 필수 API |
|---------------|-------------|---------|
| Phase 1 | Backend Phase 1 | `POST/GET/PATCH/DELETE /tenant`, `GET /tenant/list` |
| Phase 2 | Backend 기존 | `POST /ses/text-mail`, `POST /ses/templated-mail`, `/ses/template*` |
| Phase 3 | Backend 기존 | `POST/GET/PUT/DELETE /scheduler/job*` |
| Phase 4 | Backend Phase 2, 4 | `/ses/identity*`, `/onboarding*` |
| Phase 5 | Backend Phase 3, 5 | `/tenant/{id}/quota`, `/tenant/{id}/suppression`, `/ses/callback*` |

---

## 아키텍처 흐름 요약

```
[Admin UI (React)]
       │
       │ API Key via Authorization header
       ▼
[Backend API Server :7092]
       │
       ├── /tenant/* ──────────→ [RDB: Tenant Table]
       │
       ├── /ses/text-mail ──────→ [DB Insert (SR)]
       │                              │
       │                        [Polling Service]
       │                              │
       │                        [Scheduler (Quartz)]
       │                              │
       │                        [AWS SES]
       │                              │
       │                        [DynamoDB Events]
       │                              │
       │                        [Status Update]
       │
       ├── /scheduler/* ────────→ [Quartz Scheduler]
       │
       └── /onboarding/* ───────→ [AWS SES Identity]
                                  [DNS Verification]
```

---

*본 문서는 Joins EMS 프론트엔드 개발의 전체 계획을 담고 있습니다. 각 Phase는 백엔드 개발 진행 상황에 따라 순차적으로 진행하며, 공통 기반 구조는 Phase 1 시작 전 반드시 완료되어야 합니다.*
