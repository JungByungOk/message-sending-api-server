# Frontend Specification

> 최종 업데이트: 2026-04-10

---

## 기술 스택

| 항목 | 버전/라이브러리 |
|------|----------------|
| Framework | React 19.2 |
| Build Tool | Vite 8 |
| Language | TypeScript 5.9 |
| UI Library | Ant Design 5.29 + @ant-design/pro-components |
| 서버 상태 | TanStack Query (staleTime: 30s, retry: 1) |
| 클라이언트 상태 | Zustand |
| 라우팅 | React Router v6 |
| HTTP Client | Axios (apiClient) |
| 날짜 처리 | dayjs (UTC → Local 변환) |

---

## 디렉토리 구조

```
frontend/src/
├── api/            # API 호출 함수 (axios 기반)
├── components/     # 공통 컴포넌트
├── hooks/          # TanStack Query 커스텀 훅
├── layouts/        # 레이아웃 컴포넌트
├── pages/          # 페이지 컴포넌트 (feature 단위)
│   ├── auth/
│   ├── dashboard/
│   ├── email/
│   ├── onboarding/
│   ├── scheduler/
│   ├── settings/
│   ├── suppression/
│   ├── template/
│   └── tenant/
├── stores/         # Zustand 전역 상태
├── theme/          # Ant Design 테마 설정
└── types/          # TypeScript 타입 정의
```

---

## 라우팅

| Path | Component | 설명 |
|------|-----------|------|
| `/login` | `LoginPage` | JWT 로그인 |
| `/` | `DashboardPage` | 대시보드 |
| `/tenant` | `TenantList` | 테넌트 목록 |
| `/tenant/create` | `TenantCreate` | 테넌트 생성 |
| `/tenant/:id` | `TenantDetail` | 테넌트 상세/편집 |
| `/email/send` | `SendEmailPage` | 이메일 발송 테스트 |
| `/email/results` | `EmailResults` | 발송 결과 조회 |
| `/template` | `TemplateListPage` | 템플릿 관리 |
| `/scheduler` | `SchedulerPage` | 예약 발송 관리 |
| `/onboarding` | `OnboardingWizard` | 온보딩 마법사 |
| `/onboarding/:tenantId` | `OnboardingStatus` | 온보딩 상태 확인 |
| `/suppression` | `SuppressionList` | 수신 거부 목록 |
| `/settings` | `SettingsPage` | 설정 (비밀번호 변경, AWS 연동, 폴링 주기, 테마) |

잘못된 경로는 `/`로 리다이렉트합니다.

---

## 타입 정의

### src/types/auth.ts

```typescript
export interface LoginRequest { username: string; password: string; }
export interface LoginResponse { accessToken: string; refreshToken: string; user: UserInfo; }
export interface UserInfo { userId: number; username: string; displayName: string; role: string; }
export interface TokenResponse { accessToken: string; }
export interface ChangePasswordRequest { currentPassword: string; newPassword: string; }
export interface CreateUserRequest { username: string; password: string; displayName: string; role: string; }
export interface UpdateUserRequest { displayName?: string; role?: string; isActive?: boolean; }
```

### src/types/emailResults.ts

```typescript
export type EmailResultStatus =
  | 'Queued'      // 발송 큐 진입
  | 'Sending'     // SES API 호출 완료
  | 'Delayed'     // 일시적 전달 지연
  | 'Timeout'     // 1시간 이상 Sending 유지 — 자동 타임아웃
  | 'Delivered'   // 수신 MTA 전달 확인
  | 'Bounced'     // 반송
  | 'Complained'  // 수신자 스팸 신고
  | 'Rejected'    // SES 발송 거부
  | 'Error'       // 시스템 오류
  | 'Blocked';    // 내부 차단

export interface EmailResult {
  emailSendDtlSeq: number;
  emailSendSeq: number;
  sendStsCd: EmailResultStatus;
  sendRsltTypCd: string | null;   // SES 이벤트 원본값 (Delivery, Bounce, Open, Click 등)
  rcvEmailAddr: string;
  sendEmailAddr: string;
  emailTitle: string | null;
  emailTmpletId: string | null;
  correlationId: string | null;   // Backend 생성 UUID 추적 ID
  sesMessageId: string | null;    // SES 발급 실제 메시지 ID
  sesRealSendDt: string | null;
  stmFirRegDt: string;
  stmLastUpdDt: string;
}

export interface EmailResultMaster {
  emailSendSeq: number;
  emailTypCd: string | null;
  emailClsCd: string | null;
  sendDivCd: string | null;
  rsvSendDt: string | null;
  tenantId: string;
  stmFirRegDt: string;
  stmLastUpdDt: string;
  details: EmailResult[];
}

export interface EmailResultsParams {
  tenantId?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface EmailResultsResponse {
  totalCount: number;
  results: EmailResult[];
}
```

**변경 이력 (2026-04-08)**:
- `sesMsgId` 제거 — SQS Message ID로 결과 매칭 불가 확인으로 제거
- `correlationId` 추가 — Backend 생성 UUID 추적 ID (결과 상세 조회 키)
- `sesMessageId` 추가 — SES가 발급한 실제 메시지 ID

### src/types/email.ts

```typescript
export interface MessageTag {
  name: string;
  value: string;
}

export interface SendEmailRequest {
  from: string;
  to: string[];
  subject: string;
  body: string;
  tags?: MessageTag[];
}

export interface SendTemplatedEmailRequest {
  templateName: string;
  from: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  templateData: Record<string, string>;
  tags?: MessageTag[];
}

export interface SendEmailResponse {
  messageId: string;
}
```

### src/types/api.ts

```typescript
export interface ApiError {
  status: number;
  error: string;
  message: string;
}

export interface PageParams {
  page?: number;
  size?: number;
}
```

---

## 공통 컴포넌트

### StatusTag (`src/components/StatusTag.tsx`)

Adobe Spectrum 스타일 색상 팔레트 기반 상태 표시 태그 컴포넌트.

**Props**
```typescript
interface StatusTagProps {
  type: 'tenant' | 'verification' | 'job' | 'suppression' | 'emailResult';
  status: string;
  style?: CSSProperties;
}
```

**emailResult 타입 상태 매핑**

| status | 색상 | 라벨 | 아이콘 |
|--------|------|------|--------|
| `Queued` | neutral (회색) | 대기 | ClockCircleFilled |
| `Sending` | info (파랑) | 발송중 | PlayCircleFilled |
| `Delayed` | warning (주황) | 지연 | PauseCircleFilled |
| `Timeout` | warning (주황) | 타임아웃 | ExclamationCircleFilled |
| `Delivered` | positive (녹색) | 전달완료 | CheckCircleFilled |
| `Bounced` | warning (주황) | 반송 | ExclamationCircleFilled |
| `Complained` | negative (빨강) | 수신거부 | CloseCircleFilled |
| `Rejected` | negative (빨강) | 발송거부 | StopFilled |
| `Error` | negative (빨강) | 오류 | CloseCircleFilled |
| `Blocked` | negative (빨강) | 차단 | StopFilled |

**변경 이력 (2026-04-08)**: 기존 2글자 단축코드(SR/SQ/SM/SS/SF/SD/SB/SC) → 풀네임 상태 코드 체계로 전환

**변경 이력 (2026-04-09)**: `Timeout` 상태 추가 — warning(주황) 스타일, "타임아웃" 라벨

### AuthGuard (`src/components/AuthGuard.tsx`)

JWT 인증 라우트 가드 컴포넌트.
- 미인증 시 `/login`으로 리다이렉트
- 30분 비활동 자동 로그아웃 (mousedown, keydown, scroll, touchstart 이벤트 감지)
- 1분 주기 비활동 체크

---

## 대시보드 카드 (`src/pages/dashboard/`)

| 카드 | 설명 | API |
|------|------|-----|
| 발송 통계 | 전체/성공/실패 발송 건수 요약 | `GET /email/results` |
| 이메일 발송 일간 한도 | SES 일간 발송 한도 사용률 Progress bar + 잔여 건수 표시 | `GET /monitoring/ses-quota` |

**이메일 발송 일간 한도 카드** (`2026-04-09` 추가):
- `GET /monitoring/ses-quota` 호출 → Lambda `tenant-setup GET_ACCOUNT` 액션
- 응답 필드: `maxSendRate`, `max24HourSend`, `sentLast24Hours`
- 사용률(%) = `sentLast24Hours / max24HourSend * 100`, Progress bar로 시각화
- 잔여 건수 = `max24HourSend - sentLast24Hours`

---

## 페이지별 주요 기능

### EmailResults (`src/pages/email/EmailResults.tsx`)

발송 결과 조회 페이지.

**주요 기능**:
- 테넌트 / 기간 / 상태(Segmented) 필터링
- 요약 통계 카드 (전달완료 / 반송 / 수신거부)
- ProTable 페이지네이션 (0-based page)
- 행 클릭 시 `correlationId` 기반 상세 Drawer 표시
- 추적 ID(`correlationId`) 컬럼 — 단축 표시(앞8자...뒤8자) + 클립보드 복사

**상태 필터 옵션**

```typescript
const STATUS_SEGMENTS = [
  { label: '전체', value: 'ALL' },
  { label: '발송중', value: 'Sending' },
  { label: '전달완료', value: 'Delivered' },
  { label: '반송', value: 'Bounced' },
  { label: '수신거부', value: 'Complained' },
  { label: '실패', value: 'Error' },
];
```

**결과 유형 한글 레이블**

```typescript
const RESULT_TYPE_LABELS: Record<string, string> = {
  Send: '발송 수락',
  Delivery: '전달 성공',
  Open: '열람',
  Click: '클릭',
  Bounce: '반송',
  Complaint: '스팸 신고',
  Reject: '발송 거부',
  RenderingFailure: '렌더링 오류',
  DeliveryDelay: '전달 지연',
  Blacklist: '블랙리스트 차단',
  SESFail: 'SES 호출 실패',
  QuartzFail: '스케줄러 실패',
};
```

**상세 Drawer**:
- `correlationId`로 `/email/results/{correlationId}` API 호출
- 표시 필드: 추적 ID, SES 메시지 ID, 테넌트 ID, 발신자, 수신자, 제목, 상태(StatusTag), 결과 유형, 발송시각
- Bounced/Complained 상태 시 수신 거부 목록 바로가기 버튼 표시

---

## API 함수 (`src/api/`)

### emailResults.ts

```typescript
// 발송 결과 목록 조회
getEmailResults(params: EmailResultsParams): Promise<EmailResultsResponse>
// GET /email/results

// 발송 결과 상세 조회 (correlationId로 조회)
getEmailResultDetail(correlationId: string): Promise<EmailResultMaster>
// GET /email/results/{correlationId}
```

### auth.ts

```typescript
authApi.login(data: LoginRequest): Promise<LoginResponse>       // POST /auth/login
authApi.refresh(refreshToken: string): Promise<TokenResponse>    // POST /auth/refresh
authApi.logout(): Promise<void>                                  // POST /auth/logout
authApi.changePassword(data: ChangePasswordRequest): Promise<void> // POST /auth/change-password
authApi.getMe(): Promise<UserInfo>                               // GET /users/me
authApi.getUsers(): Promise<UserInfo[]>                          // GET /users
authApi.createUser(data: CreateUserRequest): Promise<UserInfo>   // POST /users
authApi.updateUser(userId: number, data: UpdateUserRequest): Promise<UserInfo> // PUT /users/:id
authApi.deleteUser(userId: number): Promise<void>                // DELETE /users/:id
```

---

## 상태 관리

| 구분 | 도구 | 용도 |
|------|------|------|
| 서버 상태 | TanStack Query | API 데이터 캐싱, 로딩/에러 상태 |
| UI 상태 | Zustand (`stores/ui.ts`) | 사이드바 collapsed 등 |
| 인증 상태 | Zustand (`stores/auth.ts`) | JWT (accessToken, refreshToken, user, lastActivity) |
| 테마 상태 | Zustand (`stores/theme.ts`) | 테마 선택 |

TanStack Query 기본 설정:
- `staleTime`: 30초
- `retry`: 1회

---

## 개발 패턴

### 날짜 처리
- API 응답의 날짜는 UTC로 가정하고 `dayjs.utc(...).local()` 로 로컬 시간 변환
- 표시 포맷: `YYYY-MM-DD HH:mm:ss`

### 페이지네이션
- Backend: 0-based page (`page=0`이 첫 페이지)
- Ant Design Pagination: 1-based — 표시 시 `page + 1`, 변경 시 `p - 1`

### ID 표시
- UUID(`correlationId`) 등 긴 ID는 단축 표시: `앞8자...뒤8자`
- Tooltip으로 전체 값 표시, 복사 버튼 제공
