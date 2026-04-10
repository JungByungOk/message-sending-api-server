# Joins EMS - Frontend 마이그레이션 계획

**작성일:** 2026-04-10
**상태:** 최종 설계
**참조:** [ses-native-migration.md](./ses-native-migration.md)

---

## 1. 현재 Frontend 구조 (As-Is)

### 1.1 기술 스택

| 라이브러리 | 용도 |
|----------|------|
| React 19 | UI 프레임워크 |
| TypeScript 5.9 | 타입 안전성 |
| Vite 7 | 빌드 도구 |
| Ant Design + Pro Components | UI 컴포넌트 |
| TanStack Query | 서버 상태 관리 |
| Zustand | 클라이언트 상태 관리 |
| Recharts | 차트/그래프 시각화 |
| Axios | HTTP 클라이언트 |

### 1.2 페이지 구성

| 경로 | 페이지 | 주요 기능 |
|------|--------|----------|
| `/` | Dashboard | KPI 카드, 최근 발송, 캠페인 통계, SES 한도, 테넌트 평판 |
| `/email/send` | SendEmail | 일반/템플릿 이메일 발송, 예약 발송 |
| `/email/results` | EmailResults | 발송 상태 추적, 필터링, 상세 드로어 |
| `/template` | TemplateList | 템플릿 CRUD, HTML 미리보기, 테넌트 매핑 |
| `/scheduler` | SchedulerPage | Quartz 예약 작업 관리 (일시정지/재개/삭제) |
| `/tenant` | TenantList | 테넌트 목록, 활성화/비활성화/삭제 |
| `/tenant/create` | TenantCreate | 테넌트 생성, 쿼터 설정, 발신자 등록 |
| `/tenant/:id` | TenantDetail | 테넌트 상세, 발신자 관리, DKIM, 인증 |
| `/suppression` | SuppressionList | 수신 거부 목록, 테넌트별 필터링 |
| `/monitoring` | MonitoringPage | 시간대별/상태별/트렌드 차트, 배치 현황 |
| `/cost` | CostPage | AWS 서비스별 추정 비용 |
| `/settings` | SettingsPage | API Key, AWS 설정, 콜백, 테마, 초기화 |

### 1.3 API 계층

| 파일 | API 수 | 호출 경로 |
|------|--------|---------|
| `api/email.ts` | 9개 | `/ses/*` |
| `api/emailResults.ts` | 2개 | `/ses/email-results`, `/ses/email-result-detail` |
| `api/monitoring.ts` | 10개 | `/monitoring/*` |
| `api/tenant.ts` | 11개 | `/tenant/*` |
| `api/tenantSetup.ts` | 7개 | `/tenant-setup/*` |
| `api/scheduler.ts` | 7개 | `/quartz/*` |
| `api/settings.ts` | 5개 | `/settings/*` |
| `api/suppression.ts` | 2개 | `/suppression/*` |
| `api/quota.ts` | 2개 | `/quota/*` |

### 1.4 상태 관리

| Store | 용도 |
|-------|------|
| `auth.ts` | API Key 저장 (localStorage 영속화) |
| `theme.ts` | 테마 설정 (6가지 테마, localStorage 영속화) |

---

## 2. Phase별 Frontend 변경 계획

### Phase 1: 코드 품질 정비 — Frontend 변경 없음

Backend DTO 통일 시 JSON 필드명이 동일하면 프론트엔드 변경 불필요. 단, Backend 응답 형식이 변경되면 타입 정의만 업데이트.

---

### Phase 2: Callback 제거 + EventBridge + SES Multi-Tenant

#### 2.1 설정 페이지 (AwsSettings.tsx)

**제거:**
- Callback 수신 설정 섹션 (URL, Secret 입력)
- `deliveryMode` 선택 (callback/polling 전환)

**추가:**
- 폴링 주기 설정 (1~10분, 숫자 입력 또는 슬라이더)

**변경 전:**
```
┌─ AWS 설정 ──────────────────────────────┐
│  API Gateway 연동                        │
│  콜백 수신 설정 (URL, Secret)   ← 제거    │
│  Polling 간격                            │
│  연결 테스트                              │
└──────────────────────────────────────────┘
```

**변경 후:**
```
┌─ AWS 설정 ──────────────────────────────┐
│  API Gateway 연동                        │
│  폴링 주기 설정 (1~10분)        ← 개선    │
│  연결 테스트                              │
└──────────────────────────────────────────┘
```

#### 2.2 테넌트 상세 (TenantDetail.tsx)

**추가:**
- SES 전송 상태 표시 (활성/일시정지/강제정지)
- 전송 일시정지/재개 버튼
- 평판 정책 표시 및 변경 (Standard/Strict/None)
- SES 전송 요약 (24시간/7일 발송량)
- 평판 조사 결과 목록

**변경 후 UI:**
```
┌─ 테넌트 상세 ────────────────────────────┐
│  기본 정보 (테넌트명, 도메인, 상태)          │
│                                          │
│  ┌─ SES 전송 상태 ─────────────── 신규 ─┐│
│  │  전송 상태: 활성화됨                    ││
│  │  [일시정지] 버튼                       ││
│  │  24시간 발송: 1,234건                  ││
│  │  7일 발송: 8,567건                     ││
│  └────────────────────────────────────┘│
│                                          │
│  ┌─ 평판 관리 ─────────────────── 신규 ─┐│
│  │  평판 정책: Standard ▼                ││
│  │  평판 상태: 발견된 조사 결과 없음        ││
│  │  평판 조사 결과 (0건)                  ││
│  └────────────────────────────────────┘│
│                                          │
│  쿼터 관리 (기존)                         │
│  발신자 관리 (기존)                        │
│  DKIM 레코드 (기존)                       │
└──────────────────────────────────────────┘
```

#### 2.3 테넌트 목록 (TenantList.tsx)

**수정:**
- 상태 컬럼에 SES 전송 상태 반영 (활성/일시정지/강제정지)
- StatusTag에 SES 전송 상태 타입 추가

#### 2.4 타입 변경

**`types/settings.ts` 수정:**
```typescript
// 제거
callbackUrl: string;
callbackSecret: string;
deliveryMode: string;

// 추가
pollingIntervalMinutes: number;
```

**`types/tenant.ts` 수정:**
```typescript
// Tenant 인터페이스에 추가
sesTenantName: string | null;
sesStatus: 'ENABLED' | 'PAUSED' | 'ENFORCED' | 'REINSTATED' | null;
reputationPolicy: 'Standard' | 'Strict' | 'None' | null;
```

#### 2.5 API 변경

**`api/settings.ts` 수정:**
- Callback 관련 필드 제거
- `PUT /settings/polling-interval` 추가

**`api/tenant.ts` 추가:**
```typescript
pauseTenant(tenantId: string)    → POST /tenant/{id}/pause
resumeTenant(tenantId: string)   → POST /tenant/{id}/resume
getTenantSesStatus(tenantId: string) → GET /tenant/{id}/ses-status
updateReputationPolicy(tenantId: string, policy: string) → PUT /tenant/{id}/reputation-policy
```

#### 2.6 Hook 변경

**`hooks/useTenants.ts` 추가:**
```typescript
usePauseTenant()           // 테넌트 일시정지 mutation
useResumeTenant()          // 테넌트 재개 mutation
useTenantSesStatus(id)     // SES 전송 상태 조회
useUpdateReputationPolicy() // 평판 정책 변경 mutation
```

**`hooks/useSettings.ts` 추가:**
```typescript
useUpdatePollingInterval()  // 폴링 주기 변경 mutation
```

---

### Phase 3: 대용량 발송 파이프라인

#### 3.1 이메일 발송 (SendEmail.tsx)

**수정:**
- 발송 API 변경: `/ses/text-mail`, `/ses/templated-mail` → `/email-enqueue` 통합
- 대량 발송 시 S3 업로드 + S3 key 전달 방식 지원
- 발송 완료 후 배치 진행률 페이지로 이동

**변경 후 발송 흐름:**
```
수신자 입력/파일 업로드
  │
  ├── ≤ 1,000건: body에 수신자 포함
  └── > 1,000건: S3 업로드 후 s3Key 전달
  │
  ▼
POST /email-enqueue → 발송 시작
  │
  ▼
배치 진행률 페이지로 이동
```

#### 3.2 발송 결과 (EmailResults.tsx)

**추가:**
- 배치 진행률 실시간 표시 (Progress Bar)
- 진행 상태: 대기 → 발송중 → 완료 (SQS 메시지 수 + DynamoDB 상태 기반)

#### 3.3 스케줄러 (SchedulerPage.tsx) — 전면 수정

**변경:**
- Quartz API → EventBridge Scheduler API로 전환
- 작업 상태: Quartz 상태 → Scheduler 상태 (ACTIVE/PAUSED/COMPLETED)

**변경 전:**
```
Quartz 기반:
  POST /quartz/schedule   → 작업 생성
  GET  /quartz/jobs       → 작업 목록
  POST /quartz/pause      → 일시 정지
  POST /quartz/resume     → 재개
  POST /quartz/delete     → 삭제
```

**변경 후:**
```
EventBridge Scheduler 기반:
  POST /scheduler/create       → 예약 생성
  GET  /scheduler/list         → 예약 목록
  POST /scheduler/{id}/pause   → 일시 정지
  POST /scheduler/{id}/resume  → 재개
  DELETE /scheduler/{id}       → 삭제
  GET  /scheduler/{id}/status  → 상태 조회
```

#### 3.4 타입 변경

**`types/scheduler.ts` 전면 수정:**
```typescript
// 제거: Quartz 관련 타입
// JobInfo, ScheduleJobRequest, JobControlRequest, AllJobsResponse

// 추가: EventBridge Scheduler 타입
export interface ScheduleRequest {
  name: string;
  tenantId: string;
  templateName: string;
  from: string;
  scheduledAt: string;       // ISO 8601 (일회성)
  cronExpression?: string;   // 반복 (선택)
  recipients?: Recipient[];  // 소량
  s3Key?: string;            // 대량
}

export interface ScheduleInfo {
  id: string;
  name: string;
  tenantId: string;
  templateName: string;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED';
  scheduledAt: string;
  lastExecutedAt: string | null;
  nextExecutionAt: string | null;
  totalRecipients: number;
}

export interface ScheduleListResponse {
  schedules: ScheduleInfo[];
  totalCount: number;
}
```

**`types/email.ts` 수정:**
```typescript
// 추가: 대량 발송 요청
export interface EnqueueEmailRequest {
  tenantId: string;
  templateName: string;
  from: string;
  recipients?: Recipient[];  // 소량 (≤1,000)
  s3Key?: string;            // 대량 (>1,000)
}

export interface Recipient {
  to: string;
  templateData: Record<string, string>;
}

export interface EnqueueResponse {
  batchId: string;
  totalCount: number;
  status: 'QUEUED';
}
```

#### 3.5 API 변경

**`api/email.ts` 수정:**
```typescript
// 제거
sendEmail()           // POST /ses/text-mail
sendTemplatedEmail()  // POST /ses/templated-mail

// 추가
enqueueEmail()        // POST /email-enqueue (즉시 + 대량 통합)
getBatchProgress()    // GET /email/batch/{batchId}/progress
```

**`api/scheduler.ts` 전면 수정:**
```typescript
// 제거: /quartz/* 전체

// 추가: /scheduler/*
createSchedule()          // POST /scheduler/create
getSchedules()            // GET /scheduler/list
pauseSchedule(id)         // POST /scheduler/{id}/pause
resumeSchedule(id)        // POST /scheduler/{id}/resume
deleteSchedule(id)        // DELETE /scheduler/{id}
getScheduleStatus(id)     // GET /scheduler/{id}/status
```

#### 3.6 Hook 변경

**`hooks/useEmail.ts` 수정:**
```typescript
// 제거
useSendEmail()
useSendTemplatedEmail()

// 추가
useEnqueueEmail()       // 발송 큐 등록 mutation
useBatchProgress(batchId) // 배치 진행률 조회 (폴링)
```

**`hooks/useScheduler.ts` 전면 수정:**
```typescript
// 제거: useJobs, useCreateJob, usePauseJob, useResumeJob, useStopJob, useDeleteJob, useDeleteAllJobs

// 추가
useSchedules()             // 예약 목록 조회
useCreateSchedule()        // 예약 생성 mutation
usePauseSchedule()         // 예약 일시 정지 mutation
useResumeSchedule()        // 예약 재개 mutation
useDeleteSchedule()        // 예약 삭제 mutation
useScheduleStatus(id)      // 예약 상태 조회
```

---

### Phase 4: 모니터링 / 비용 / Suppression / 설정

#### 4.1 대시보드 (Dashboard/index.tsx)

**수정:**
- SES 한도 카드: 기존 유지 (SES GetAccount 데이터)
- 테넌트 평판 섹션: CloudWatch 테넌트 메트릭 기반으로 전환
- 캠페인 통계: CloudWatch 데이터 병합

#### 4.2 모니터링 (MonitoringPage.tsx)

**수정:**
- 시간대별/상태별 차트: 로컬 DB → CloudWatch 메트릭 기반
- 데이터 소스 표시 라벨 추가 ("CloudWatch 실시간" vs "로컬 DB")

#### 4.3 비용 (CostPage.tsx)

**수정:**
- 하드코딩 추정 비용 → Cost Explorer 실 비용
- 라벨 구분: "실제 비용" (Cost Explorer) vs "추정 비용" (폴백)
- 서비스별 실 비용 표시

**변경 후 UI:**
```
┌─ 비용 모니터링 ──────────────────────────┐
│                                          │
│  이번 달 발송: 32,000건                   │
│  이번 달 비용: $12.45 (실제 비용)    ← 변경│
│                                          │
│  서비스별 비용 (Cost Explorer 기준) ← 변경 │
│  ├── SES: $3.20                          │
│  ├── Lambda: $0.85                       │
│  ├── DynamoDB: $1.20                     │
│  └── ...                                 │
│                                          │
└──────────────────────────────────────────┘
```

#### 4.4 수신 거부 (SuppressionList.tsx)

**수정:**
- 2계층 구조 UI: 전체 Suppression + 테넌트별 Suppression 탭 분리
- 전체 Suppression은 관리자만 삭제 가능
- 테넌트 Suppression 삭제 시 경고 ("전체 목록에 남아있으면 여전히 차단됨")

**변경 후 UI:**
```
┌─ 수신 거부 목록 ─────────────────────────┐
│                                          │
│  [전체 Suppression] [테넌트별]   ← 탭 추가│
│                                          │
│  전체 Suppression 탭:                     │
│  ├── 전체 차단 이메일 목록 (계정 수준)      │
│  └── 관리자만 삭제 가능                    │
│                                          │
│  테넌트별 탭:                              │
│  ├── 테넌트 선택 드롭다운                   │
│  ├── 해당 테넌트의 차단 이메일 목록          │
│  └── 삭제 시 경고 메시지                   │
│                                          │
└──────────────────────────────────────────┘
```

#### 4.5 설정 (Settings/index.tsx)

**추가:**
- VDM ON/OFF 토글 (즉시 반영, 비용 안내)

**변경 후 UI:**
```
┌─ 설정 ──────────────────────────────────┐
│                                          │
│  시스템 정보 (기존)                        │
│  API Key 관리 (기존)                      │
│                                          │
│  ┌─ VDM 설정 ─────────────────── 신규 ─┐│
│  │  Virtual Deliverability Manager       ││
│  │  ┌────┐                              ││
│  │  │ ON │  ← 토글                       ││
│  │  └────┘                              ││
│  │  ISP별 전달률 인사이트를 제공합니다.     ││
│  │  활성화 시 $0.07/1,000건 추가 비용      ││
│  └────────────────────────────────────┘│
│                                          │
│  AWS 설정 (기존, Callback 제거됨)          │
│  테마 설정 (기존)                         │
│  Danger Zone (기존)                       │
└──────────────────────────────────────────┘
```

#### 4.6 타입 변경

**`types/monitoring.ts` 추가:**
```typescript
export interface CloudWatchMetric {
  metricName: string;
  tenantName: string;
  datapoints: { timestamp: string; value: number }[];
}

// CostEstimate 수정
export interface CostEstimate {
  // 기존 필드 유지
  isActual: boolean;  // true: Cost Explorer 실 비용, false: 추정
}
```

**`types/suppression.ts` 수정:**
```typescript
export interface SuppressionEntry {
  id: number;
  email: string;
  reason: 'BOUNCE' | 'COMPLAINT';
  tenantId: string | null;  // null이면 전체 Suppression
  createdAt: string;
}
```

**`types/settings.ts` 추가:**
```typescript
vdmEnabled: boolean;
pollingIntervalMinutes: number;
```

#### 4.7 API 변경

**`api/monitoring.ts` 추가:**
```typescript
getCloudWatchMetrics(tenantId, period)  // GET /monitoring/cloudwatch
getActualCost(months)                  // GET /monitoring/cost/actual
```

**`api/suppression.ts` 수정:**
```typescript
getGlobalSuppressions(params)     // GET /suppression/global
removeGlobalSuppression(email)    // DELETE /suppression/global/{email}
// 기존 테넌트별 API 유지
```

**`api/settings.ts` 추가:**
```typescript
updateVdm(enabled: boolean)              // PUT /settings/vdm
updatePollingInterval(minutes: number)   // PUT /settings/polling-interval
```

#### 4.8 Hook 변경

**`hooks/useMonitoring.ts` 추가:**
```typescript
useCloudWatchMetrics(tenantId, period)  // CloudWatch 메트릭 조회
useActualCost(months)                   // 실 비용 조회
```

**`hooks/useSuppression.ts` 추가:**
```typescript
useGlobalSuppressions(params)     // 전체 Suppression 목록
useRemoveGlobalSuppression()      // 전체 Suppression 삭제 mutation
```

**`hooks/useSettings.ts` 추가:**
```typescript
useUpdateVdm()              // VDM ON/OFF mutation
useUpdatePollingInterval()  // 폴링 주기 변경 mutation
```

---

## 3. 메뉴 구조 변경

### As-Is

```
대시보드
서비스 관리
  ├── 테스트 이메일 발송
  ├── 발송 결과 조회
  ├── 템플릿 관리
  └── 스케줄러 등록 현황
테넌트 관리
  ├── 테넌트 목록
  └── 수신 거부
모니터링
AWS 비용
설정
```

### To-Be (변경 없음)

메뉴 구조는 유지. 내부 기능만 변경.

---

## 4. 변경 규모 요약

### Phase별 파일 변경

| Phase | 신규 파일 | 수정 파일 | 삭제 항목 |
|-------|---------|---------|----------|
| Phase 2 | 0 | 5~6개 | Callback 설정 UI, 관련 타입 |
| Phase 3 | 1~2개 | 4~5개 | Quartz 타입/API/Hook 전체 |
| Phase 4 | 1~2개 | 6~7개 | - |
| **합계** | **3~4개** | **15~18개** | Quartz/Callback 관련 |

### 파일별 변경 매트릭스

| 파일 | Phase 2 | Phase 3 | Phase 4 |
|------|:-------:|:-------:|:-------:|
| **Pages** | | | |
| `pages/dashboard/index.tsx` | | | 수정 |
| `pages/email/SendEmail.tsx` | | 수정 | |
| `pages/email/EmailResults.tsx` | | 수정 | |
| `pages/scheduler/SchedulerPage.tsx` | | **전면 수정** | |
| `pages/tenant/TenantList.tsx` | 수정 | | |
| `pages/tenant/TenantDetail.tsx` | 수정 | | |
| `pages/monitoring/MonitoringPage.tsx` | | | 수정 |
| `pages/cost/CostPage.tsx` | | | 수정 |
| `pages/suppression/SuppressionList.tsx` | | | 수정 |
| `pages/settings/AwsSettings.tsx` | 수정 | | |
| `pages/settings/index.tsx` | | | 수정 |
| **API** | | | |
| `api/email.ts` | | 수정 | |
| `api/scheduler.ts` | | **전면 수정** | |
| `api/tenant.ts` | 수정 | | |
| `api/settings.ts` | 수정 | | 수정 |
| `api/monitoring.ts` | | | 수정 |
| `api/suppression.ts` | | | 수정 |
| **Hooks** | | | |
| `hooks/useEmail.ts` | | 수정 | |
| `hooks/useScheduler.ts` | | **전면 수정** | |
| `hooks/useTenants.ts` | 수정 | | |
| `hooks/useSettings.ts` | 수정 | | 수정 |
| `hooks/useMonitoring.ts` | | | 수정 |
| `hooks/useSuppression.ts` | | | 수정 |
| **Types** | | | |
| `types/email.ts` | | 수정 | |
| `types/scheduler.ts` | | **전면 수정** | |
| `types/tenant.ts` | 수정 | | |
| `types/settings.ts` | 수정 | | 수정 |
| `types/monitoring.ts` | | | 수정 |
| `types/suppression.ts` | | | 수정 |
| **Components** | | | |
| `components/StatusTag.tsx` | 수정 | | |

---

## 5. 주의사항

### 하위 호환성

- Phase 2: Callback 제거 시 설정 페이지 UI만 변경, 다른 페이지 영향 없음
- Phase 3: 발송/스케줄러 API가 완전히 변경되므로 Backend API 준비 후 전환
- Phase 4: 모니터링 데이터 소스 변경이지만 차트 구조는 유지

### Backend API 준비 순서

각 Phase에서 Frontend 변경 전에 Backend API가 먼저 준비되어야 한다.

```
Phase 2: Backend (테넌트 일시정지/재개 API) → Frontend (UI 추가)
Phase 3: Backend (email-enqueue, scheduler API) → Frontend (전환)
Phase 4: Backend (CloudWatch, Cost Explorer API) → Frontend (데이터 소스 전환)
```

### Feature Flag 연동

Backend의 Feature Flag (`ses.tenant.enabled`)에 따라 Frontend에서 SES 테넌트 관련 UI를 조건부 표시.

```typescript
// 예시: 테넌트 상세에서 SES 기능 조건부 표시
const { data: tenant } = useTenant(id);

{tenant.sesTenantName && (
  <>
    <SesStatusCard />
    <ReputationCard />
  </>
)}
```

---

## 6. 로그인 기능

### 6.1 인증 구조 (JWT 우선)

```
관리자 (대시보드):  JWT → 모든 API 접근 가능 (테넌트 지정 발송 포함)
외부 테넌트:       API Key → 발송 API만 접근 (자기 테넌트만)
```

**Backend 인증 처리 흐름:**

```
요청 수신
  │
  ├── Authorization: Bearer eyJhbG... (JWT 형식)
  │     └── JWT 검증 → 관리자 권한 → 모든 API 접근
  │
  ├── Authorization: Bearer sk-xxxx... (API Key 형식)
  │     └── API Key 조회 → 테넌트 식별 → 발송 API만 접근
  │
  └── 인증 헤더 없음
        └── 401 Unauthorized
```

**API 접근 권한 매트릭스:**

| API | JWT (관리자) | API Key (테넌트) |
|-----|:----------:|:--------------:|
| `/auth/*` | O | X |
| `/users/*` | O | X |
| `/tenant/*` | O | X |
| `/tenant-setup/*` | O | X |
| `/monitoring/*` | O | X |
| `/settings/*` | O | X |
| `/suppression/*` | O | X |
| `/scheduler/*` | O | X |
| `/email-enqueue` | O (테넌트 지정) | O (자기 테넌트만) |
| `/ses/templates` | O | O (자기 테넌트만) |

**Frontend 변경:**
- 로그인 → JWT 저장 → 모든 요청에 JWT 사용
- 설정 페이지의 "API Key 입력" 섹션 **제거**
- 테넌트 상세 페이지에서 "테넌트 API Key 조회/재발급"만 유지

### 6.2 JWT 흐름

```
POST /auth/login { username, password }
  → { accessToken (30분), refreshToken (7일) }

요청마다 Authorization: Bearer {accessToken}

accessToken 만료 → POST /auth/refresh → 새 accessToken 발급

미사용 30분 → 경고 모달 → 1분 후 자동 로그아웃
```

### 6.3 DB 테이블

```sql
CREATE TABLE ADM_USER_MST (
  user_id       SERIAL PRIMARY KEY,
  username      VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name  VARCHAR(100),
  role          VARCHAR(20) DEFAULT 'ADMIN',
  is_active     BOOLEAN DEFAULT true,
  last_login_at TIMESTAMP,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 초기 데이터 (bcrypt 해시)
INSERT INTO ADM_USER_MST (username, password_hash, display_name, role)
VALUES ('admin', '{bcrypt}...', 'Administrator', 'ADMIN');
```

### 6.4 API

| API | Method | 설명 |
|-----|--------|------|
| `/auth/login` | POST | 로그인 → JWT 발급 |
| `/auth/refresh` | POST | 토큰 갱신 |
| `/auth/logout` | POST | 로그아웃 (refreshToken 무효화) |
| `/auth/change-password` | PUT | 비밀번호 변경 |
| `/users` | GET | 사용자 목록 |
| `/users` | POST | 사용자 추가 |
| `/users/{id}` | PUT | 사용자 수정 |
| `/users/{id}` | DELETE | 사용자 삭제 |
| `/users/me` | GET | 내 정보 조회 |

### 6.5 Frontend 페이지

| 페이지 | 경로 | 기능 |
|--------|------|------|
| 로그인 | `/login` | 아이디/비밀번호 입력, JWT 저장 |
| 비밀번호 변경 | `/settings` 내 섹션 | 현재/신규 비밀번호 입력 |
| 사용자 관리 | `/users` (신규) | 사용자 CRUD |

### 6.6 로그인 페이지 와이어프레임

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                     ┌──────────┐                            │
│                     │ ✉ EMS   │                            │
│                     └──────────┘                            │
│                     Joins EMS                               │
│                                                             │
│              ┌────────────────────────┐                     │
│  아이디:     │ admin                  │                     │
│              └────────────────────────┘                     │
│              ┌────────────────────────┐                     │
│  비밀번호:   │ ••••••                 │                     │
│              └────────────────────────┘                     │
│                                                             │
│              ┌────────────────────────┐                     │
│              │        로그인          │                     │
│              └────────────────────────┘                     │
│                                                             │
│              © 2026 Joins EMS                               │
└─────────────────────────────────────────────────────────────┘
```

### 6.7 자동 로그아웃

```
마지막 활동 시간 기록 (클릭, 키보드, API 호출)
  │
  ├── 30분 미활동 → 경고 모달 "1분 후 자동 로그아웃됩니다"
  ├── 31분 미활동 → 자동 로그아웃 → /login 이동
  └── 활동 감지 → 타이머 리셋
```

### 6.8 라우트 보호

```typescript
// 인증되지 않은 사용자 → /login 리다이렉트
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route element={<AuthGuard />}>       {/* JWT 검증 */}
    <Route element={<MainLayout />}>
      <Route index element={<DashboardPage />} />
      ...
    </Route>
  </Route>
</Routes>
```

### 6.9 Zustand Store 변경

```typescript
// stores/auth.ts — JWT 기반으로 전면 교체
interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: { username: string; displayName: string; role: string } | null;
  lastActivity: number;

  login(accessToken: string, refreshToken: string, user: User): void;
  logout(): void;
  refreshAccessToken(newToken: string): void;
  updateLastActivity(): void;
}

// 기존 apiKey 필드 제거
// API Key는 테넌트 상세 페이지에서 조회/재발급만 지원
```

### 6.10 Axios 인터셉터 변경

```typescript
// api/client.ts — JWT 기반으로 변경
apiClient.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// 401 응답 시 토큰 갱신 시도
apiClient.interceptors.response.use(null, async (error) => {
  if (error.response?.status === 401) {
    const newToken = await refreshToken();
    if (newToken) {
      // 재시도
      error.config.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(error.config);
    }
    // 갱신 실패 → 로그아웃
    useAuthStore.getState().logout();
  }
  return Promise.reject(error);
});
```

### 6.11 설정 페이지 변경

| 항목 | As-Is | To-Be |
|------|-------|-------|
| API Key 입력 섹션 | 관리자가 API Key 직접 입력 | **제거** (JWT로 대체) |
| API Key 조회 | 설정 페이지 | **테넌트 상세 페이지**에서 테넌트별 조회/재발급 |

---

## 7. Phase별 UI 와이어프레임 상세

### Phase 2: 테넌트 상세 페이지

```
┌─ 테넌트 상세: Tenant A ─────────────────────────────────────────┐
│                                                                  │
│  ┌─ 기본 정보 ────────────────────────────────────────────────┐  │
│  │  테넌트명: Tenant A          도메인: tenant-a.com           │  │
│  │  상태: [활성] 태그            인증: [인증완료] 태그          │  │
│  │  API Key: sk-xxxx...xxxx    [복사] [재발급]                 │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ SES 전송 상태 ───────────────────────── 신규 ─────────────┐  │
│  │                                                            │  │
│  │  전송 상태    [활성화됨] 태그        [일시정지] 버튼          │  │
│  │                                                            │  │
│  │  ┌──────────────┐  ┌──────────────┐                        │  │
│  │  │ 24시간 발송   │  │ 7일 발송     │                        │  │
│  │  │   1,234건    │  │   8,567건    │                        │  │
│  │  └──────────────┘  └──────────────┘                        │  │
│  │                                                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 평판 관리 ───────────────────────────── 신규 ─────────────┐  │
│  │                                                            │  │
│  │  평판 정책: [Standard ▼]  (Standard / Strict / None)       │  │
│  │  평판 상태: 발견된 조사 결과 없음  ✅                        │  │
│  │                                                            │  │
│  │  ┌─ 평판 조사 결과 (0건) ─────────────────────────────┐    │  │
│  │  │  발견된 조사 결과가 없습니다.                        │    │  │
│  │  └────────────────────────────────────────────────────┘    │  │
│  │                                                            │  │
│  │  ┌─ 반송율/불만율 게이지 ─────────────────────────────┐    │  │
│  │  │  반송율: ██░░░░░░░░ 2.1%  (임계값 5%)              │    │  │
│  │  │  불만율: █░░░░░░░░░ 0.05% (임계값 0.1%)            │    │  │
│  │  └────────────────────────────────────────────────────┘    │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 쿼터 관리 ────────────────────────────── 기존 ────────────┐  │
│  │  일별: ████████░░ 8,200 / 10,000건 (82%)                  │  │
│  │  월별: ██████░░░░ 62,000 / 100,000건 (62%)                │  │
│  │  [쿼터 수정] 버튼                                          │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 발신자 관리 ──────────────────────────── 기존 ────────────┐  │
│  │  no-reply@tenant-a.com  [인증완료]                         │  │
│  │  info@tenant-a.com      [대기중] [재발송]                   │  │
│  │  [발신자 추가] 버튼                                        │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ DKIM 레코드 ──────────────────────────── 기존 ────────────┐  │
│  │  테이블: Name / Type / Value                               │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### Phase 3: 이메일 발송 페이지 (개편)

```
┌─ 이메일 발송 ────────────────────────────────────────────────────┐
│                                                                  │
│  ┌─ 발송 설정 ────────────────────────────────────────────────┐  │
│  │                                                            │  │
│  │  발송 유형:  ◉ 템플릿   ○ 직접 작성                         │  │
│  │                                                            │  │
│  │  테넌트:    [Tenant A ▼]                                   │  │
│  │  발신자:    [no-reply@tenant-a.com ▼]                      │  │
│  │  템플릿:    [welcome-email ▼]         [미리보기]             │  │
│  │                                                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 수신자 ───────────────────────────────────────────────────┐  │
│  │                                                            │  │
│  │  수신자 입력:  ◉ 직접 입력   ○ 파일 업로드 (CSV/Excel)      │  │
│  │                                                            │  │
│  │  [user1@example.com ×] [user2@example.com ×] [입력...]     │  │
│  │                                                            │  │
│  │  또는 CSV 업로드: [파일 선택]  (to, userName, ... 컬럼)     │  │
│  │                                                            │  │
│  │  수신자 수: 1,234명                                        │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 템플릿 변수 ──────────────────────────────────────────────┐  │
│  │  userName:    [기본값 입력] (CSV 컬럼 매핑 시 자동)          │  │
│  │  verifyLink:  [기본값 입력]                                 │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 발송 옵션 ────────────────────────────────────────────────┐  │
│  │                                                            │  │
│  │  발송 시점:  ◉ 즉시 발송   ○ 예약 발송                      │  │
│  │                                                            │  │
│  │  (예약 선택 시)                                             │  │
│  │  예약 일시:  [2026-04-15]  [09:00]                         │  │
│  │                                                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 발송 전 확인 ─────────────────────────────────────────────┐  │
│  │  테넌트: Tenant A  |  수신자: 1,234명  |  템플릿: welcome   │  │
│  │  쿼터 잔여: 1,800건 (일별)                                  │  │
│  │                                                            │  │
│  │              [발송하기]    [취소]                            │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### Phase 3: 발송 진행률 화면

```
┌─ 발송 진행 상황 ─────────────────────────────────────────────────┐
│                                                                  │
│  배치 ID: batch-20260415-001                                     │
│  테넌트: Tenant A  |  템플릿: welcome-email                      │
│  시작: 2026-04-15 09:00:12                                       │
│                                                                  │
│  ┌─ 전체 진행률 ──────────────────────────────────────────────┐  │
│  │  ████████████████████░░░░░░░░░░  68%  (845 / 1,234건)     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 발송완료  │  │ 전달완료  │  │ 반송     │  │ 대기중    │        │
│  │   845건  │  │   812건  │  │   15건   │  │   389건  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                  │
│  경과 시간: 2분 34초  |  예상 잔여: 1분 12초                      │
│  처리 속도: ~5.5건/초                                            │
│                                                                  │
│  [발송 결과 상세 보기]   [대시보드로 이동]                         │
└──────────────────────────────────────────────────────────────────┘
```

### Phase 4: 수신 거부 2계층 UI

```
┌─ 수신 거부 관리 ─────────────────────────────────────────────────┐
│                                                                  │
│  [전체 Suppression]  [테넌트별 Suppression]    ← 탭               │
│  ─────────────────────────────────────────                       │
│                                                                  │
│  === 전체 Suppression 탭 ===                                     │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                       │
│  │ 전체 차단 │  │ 반송     │  │ 스팸신고  │                       │
│  │   156건  │  │   123건  │  │    33건  │                       │
│  └──────────┘  └──────────┘  └──────────┘                       │
│                                                                  │
│  ⚠️ 전체 Suppression에서 삭제하면 모든 테넌트에서 발송 가능합니다.  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ 이메일               │ 사유     │ 등록일      │ 작업        │ │
│  ├──────────────────────┼─────────┼────────────┼────────────┤ │
│  │ bad@example.com      │ [반송]  │ 2026-04-10 │ [삭제]     │ │
│  │ spam@example.com     │ [스팸]  │ 2026-04-09 │ [삭제]     │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  === 테넌트별 Suppression 탭 ===                                  │
│                                                                  │
│  테넌트 선택: [Tenant A ▼]                                       │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ 이메일               │ 사유     │ 등록일      │ 작업        │ │
│  ├──────────────────────┼─────────┼────────────┼────────────┤ │
│  │ user@example.com     │ [반송]  │ 2026-04-10 │ [삭제]     │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ⚠️ 테넌트 Suppression에서 삭제해도 전체 목록에 있으면 차단됩니다.  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Phase 4: 설정 페이지 (VDM + 폴링)

```
┌─ 설정 ───────────────────────────────────────────────────────────┐
│                                                                  │
│  ┌─ 시스템 정보 ──────────────────────────── 기존 ────────────┐  │
│  │  API 서버: http://localhost:8080  [정상]                    │  │
│  │  빌드 모드: development  |  버전: 0.0.1                     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 비밀번호 변경 ────────────────────────── 신규 ────────────┐  │
│  │  현재 비밀번호:  [••••••       ]                            │  │
│  │  새 비밀번호:    [••••••       ]                            │  │
│  │  비밀번호 확인:  [••••••       ]  [변경]                     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 이벤트 수신 설정 ─────────────────────── 개편 ────────────┐  │
│  │                                                            │  │
│  │  폴링 주기:  [2 ▼] 분  (1~10분)                             │  │
│  │  ℹ️ DynamoDB에서 발송 결과를 가져오는 주기입니다.             │  │
│  │                                                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ VDM (Virtual Deliverability Manager) ─── 신규 ────────────┐  │
│  │                                                            │  │
│  │  ┌────┐                                                    │  │
│  │  │ ON │  ISP별 전달률 인사이트를 제공합니다.                  │  │
│  │  └────┘                                                    │  │
│  │  활성화 시 $0.07/1,000건 추가 비용이 발생합니다.              │  │
│  │                                                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ AWS 설정 ─────────────────────────────── 기존 ────────────┐  │
│  │  API Gateway Endpoint / Region / API Key                   │  │
│  │  [연결 테스트]                                              │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ 테마 설정 ────────────────────────────── 기존 ────────────┐  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ Danger Zone ──────────────────────────── 기존 ────────────┐  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 8. Backend API ↔ Frontend 타입 1:1 매핑표

### Phase 2 API

| Backend API | Method | Frontend API 함수 | Request 타입 | Response 타입 |
|-------------|--------|------------------|-------------|--------------|
| `/tenant/{id}/pause` | POST | `pauseTenant(id)` | - | `{ success: boolean }` |
| `/tenant/{id}/resume` | POST | `resumeTenant(id)` | - | `{ success: boolean }` |
| `/tenant/{id}/ses-status` | GET | `getTenantSesStatus(id)` | - | `SesStatusResponse` |
| `/tenant/{id}/reputation-policy` | PUT | `updateReputationPolicy(id, policy)` | `{ policy: string }` | `{ success: boolean }` |
| `/settings/polling-interval` | PUT | `updatePollingInterval(minutes)` | `{ intervalMinutes: number }` | `{ success: boolean }` |

**Phase 2 신규 타입:**

```typescript
// types/tenant.ts 추가
interface SesStatusResponse {
  sesTenantName: string;
  sesStatus: 'ENABLED' | 'PAUSED' | 'ENFORCED' | 'REINSTATED';
  reputationPolicy: 'Standard' | 'Strict' | 'None';
  sentLast24Hours: number;
  sentLast7Days: number;
  reputationFindings: ReputationFinding[];
}

interface ReputationFinding {
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  type: string;
  description: string;
  createdAt: string;
}
```

### Phase 3 API

| Backend API | Method | Frontend API 함수 | Request 타입 | Response 타입 |
|-------------|--------|------------------|-------------|--------------|
| `/email-enqueue` | POST | `enqueueEmail(req)` | `EnqueueEmailRequest` | `EnqueueResponse` |
| `/email/batch/{batchId}/progress` | GET | `getBatchProgress(batchId)` | - | `BatchProgress` |
| `/scheduler/create` | POST | `createSchedule(req)` | `ScheduleRequest` | `{ scheduleId: string }` |
| `/scheduler/list` | GET | `getSchedules()` | - | `ScheduleListResponse` |
| `/scheduler/{id}/pause` | POST | `pauseSchedule(id)` | - | `{ success: boolean }` |
| `/scheduler/{id}/resume` | POST | `resumeSchedule(id)` | - | `{ success: boolean }` |
| `/scheduler/{id}` | DELETE | `deleteSchedule(id)` | - | `{ success: boolean }` |
| `/scheduler/{id}/status` | GET | `getScheduleStatus(id)` | - | `ScheduleInfo` |

**Phase 3 신규 타입:**

```typescript
// types/email.ts 추가
interface EnqueueEmailRequest {
  tenantId: string;
  templateName: string;
  from: string;
  recipients?: Recipient[];    // 소량 ≤1,000
  s3Key?: string;              // 대량 >1,000
}

interface Recipient {
  to: string;
  templateData: Record<string, string>;
}

interface EnqueueResponse {
  batchId: string;
  totalCount: number;
  status: 'QUEUED';
}

interface BatchProgress {
  batchId: string;
  totalCount: number;
  sentCount: number;
  deliveredCount: number;
  bouncedCount: number;
  pendingCount: number;
  progressPercent: number;
  elapsedSeconds: number;
  estimatedRemainingSeconds: number;
  status: 'QUEUING' | 'SENDING' | 'COMPLETED' | 'FAILED';
}

// types/scheduler.ts 전면 교체
interface ScheduleRequest {
  name: string;
  tenantId: string;
  templateName: string;
  from: string;
  scheduledAt: string;         // ISO 8601
  cronExpression?: string;     // 반복 (선택)
  recipients?: Recipient[];
  s3Key?: string;
}

interface ScheduleInfo {
  id: string;
  name: string;
  tenantId: string;
  templateName: string;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED';
  scheduledAt: string;
  lastExecutedAt: string | null;
  nextExecutionAt: string | null;
  totalRecipients: number;
}

interface ScheduleListResponse {
  schedules: ScheduleInfo[];
  totalCount: number;
}
```

### Phase 4 API

| Backend API | Method | Frontend API 함수 | Request 타입 | Response 타입 |
|-------------|--------|------------------|-------------|--------------|
| `/monitoring/cloudwatch` | GET | `getCloudWatchMetrics(params)` | `{ tenantId?, period }` | `CloudWatchMetric[]` |
| `/monitoring/cost/actual` | GET | `getActualCost(months)` | `{ months: number }` | `ActualCostResponse` |
| `/suppression/global` | GET | `getGlobalSuppressions(params)` | `PageParams` | `SuppressionListResponse` |
| `/suppression/global/{email}` | DELETE | `removeGlobalSuppression(email)` | - | `{ success: boolean }` |
| `/settings/vdm` | PUT | `updateVdm(enabled)` | `{ enabled: boolean }` | `{ success: boolean }` |
| `/settings/vdm` | GET | `getVdmStatus()` | - | `{ enabled: boolean }` |

**Phase 4 신규 타입:**

```typescript
// types/monitoring.ts 추가
interface CloudWatchMetric {
  metricName: 'Send' | 'Delivery' | 'Bounce' | 'Complaint' | 'Open' | 'Click';
  tenantName: string | null;   // null이면 계정 전체
  datapoints: { timestamp: string; value: number }[];
  period: '1h' | '24h' | '7d';
}

interface ActualCostResponse {
  isActual: true;
  monthlyBreakdown: ActualCostMonthly[];
  totalCost: number;
  currency: 'USD';
}

interface ActualCostMonthly {
  month: string;
  services: { name: string; cost: number }[];
  totalCost: number;
}
```

---

## 9. 페이지별 상세 변경 사항

### 8.1 대시보드 (Dashboard/index.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 환영 배너 + SES 한도 카드 | 풀 너비 카드 2개 연속 | **단일 운영 상태 헤더로 통합** | 4 |
| 반송율 KPI | 숫자 표시 (warn 테두리) | **게이지 바 (0~5% 구간 색상 전환)** | 4 |
| 테넌트 평판 테이블 | 로컬 DB 쿼리 | **CloudWatch 테넌트 메트릭 기반** | 4 |
| 실시간 갱신 | 없음 | **60초 주기 자동 갱신** | 4 |
| 알림 배너 | 없음 | **반송율/한도 초과 시 상단 경고 배너** | 4 |

### 8.2 이메일 발송 (SendEmail.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 탭 구조 | 일반/템플릿/예약 3개 탭 | **단일 폼 + 발송유형 라디오 + 즉시/예약 전환** | 3 |
| 발송 API | `/ses/text-mail`, `/ses/templated-mail` | **`/email-enqueue` 통합** | 3 |
| 대량 수신자 | 태그 입력만 | **CSV/Excel 파일 업로드 지원** | 3 |
| 발송 후 | Result 컴포넌트 (성공/실패) | **배치 진행률 페이지로 이동** | 3 |
| 발송 전 확인 | 없음 | **수신자 수, 쿼터 잔여량 요약 표시** | 3 |

### 8.3 발송 결과 (EmailResults.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 요약 카드 | "현재 페이지 기준" | **전체 집계 기준으로 변경** | 4 |
| 배치 진행률 | 없음 | **SENDING 상태 배치 Progress Bar 표시** | 3 |

### 8.4 스케줄러 (SchedulerPage.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 데이터 소스 | Quartz API (`/quartz/*`) | **EventBridge Scheduler API (`/scheduler/*`)** | 3 |
| 작업 상태 | RUNNING/SCHEDULED/PAUSED/COMPLETE | **ACTIVE/PAUSED/COMPLETED** | 3 |
| 작업 생성 | Cron 표현식 직접 입력 | **날짜/시간 선택 UI + Cron 고급 옵션** | 3 |
| 전체 삭제 | `/quartz/delete-all` | **개별 삭제로 변경 (안전)** | 3 |

### 8.5 테넌트 목록 (TenantList.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 상태 컬럼 | ACTIVE/INACTIVE | **SES 전송 상태 포함 (활성/일시정지/강제정지)** | 2 |
| 쿼터 컬럼 | 숫자만 표시 | **미니 진행 막대 (사용량/한도)** | 4 |
| 액션 메뉴 | 상세/활성화/비활성화/삭제 | **일시정지/재개 추가, 삭제 분리** | 2 |

### 8.6 테넌트 상세 (TenantDetail.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| SES 전송 상태 | 없음 | **전송 상태 카드 + 일시정지/재개 버튼** | 2 |
| 평판 관리 | 없음 | **평판 정책 선택 + 조사 결과 + 반송율/불만율 게이지** | 2 |
| 발신자 인증 | 개별 API 호출 (N+1) | **Batch API 권장 (Backend 협업)** | 4 |

### 8.7 수신 거부 (SuppressionList.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 구조 | 단일 목록 + 테넌트 필터 | **탭: 전체 Suppression / 테넌트별** | 4 |
| 전체 목록 | 없음 | **계정 수준 Suppression (관리자 전용 삭제)** | 4 |
| 삭제 경고 | 없음 | **"전체 목록에 있으면 여전히 차단" 경고** | 4 |

### 8.8 모니터링 (MonitoringPage.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 데이터 소스 | 로컬 DB 자체 쿼리 | **CloudWatch 메트릭 + 로컬 DB 병합** | 4 |
| 실시간 갱신 | 없음 | **오늘 날짜 시 60초 자동 갱신** | 4 |
| 도넛 차트 | 중앙 빈 공간 | **중앙에 총 건수 표시** | 4 |
| 배치 현황 | 상태만 표시 | **Progress Bar 추가** | 3 |

### 8.9 비용 (CostPage.tsx)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| 데이터 | 하드코딩 단가 추정 | **Cost Explorer 실 비용 (폴백: 추정)** | 4 |
| 라벨 | "추정 비용" | **"실제 비용" vs "추정 비용" 구분** | 4 |

### 8.10 설정 (Settings)

| 항목 | As-Is | To-Be | Phase |
|------|-------|-------|-------|
| Callback 설정 | URL, Secret, deliveryMode | **제거** | 2 |
| 폴링 주기 | 있음 | **1~10분 입력 (기본 2분)** | 2 |
| VDM 토글 | 없음 | **ON/OFF + 비용 안내** | 4 |

---

## 10. UX 고도화 개선 제안

### 9.1 글로벌 알림 시스템 (최우선)

대용량 발송 솔루션의 가장 큰 UX 결함: **이상 징후를 능동적으로 알리는 메커니즘이 없음.**

**구현:**
- `MainLayout.tsx` 헤더에 알림 벨 아이콘 추가
- `AlertWatcher` 컴포넌트: 주기적으로 반송율/SES 한도 감지
- 알림 조건과 표시 방식:

| 조건 | 표시 | 닫기 |
|------|------|------|
| 반송율 2~5% | 노란 배너 | 가능 |
| 반송율 5% 초과 | 빨간 배너 | 불가 |
| SES 한도 80% | 노란 배너 | 가능 |
| SES 한도 100% | 빨간 배너 + 발송 차단 안내 | 불가 |
| 배치 발송 실패 | notification.error 팝업 | 자동 |
| 테넌트 자동 일시정지 | notification.warning 팝업 | 자동 |

### 9.2 메뉴 구조 개선

**As-Is:**
```
서비스 관리
  └── 테스트 이메일 발송   ← "테스트" 혼동
```

**To-Be:**
```
이메일 발송
  └── 이메일 발송          ← 명확한 레이블
```

### 9.3 데이터 시각화 개선

| 개선 | 현재 | 제안 |
|------|------|------|
| 도넛 차트 | 중앙 빈 공간 | 중앙에 총 건수 표시 |
| 반송율 KPI | 숫자 + warn 테두리 | 게이지 바 (SES 임계값 5% 대비 위치) |
| 상태 색상 | STATUS_COLORS 2곳 중복 정의 | `constants/statusColors.ts`로 통합 |
| 시간대별 차트 | interval=2 (짝수만 표시) | interval=1 (전체 표시) |
| 트렌드 이중 Y축 | 레이블 없음 | 축 레이블 명시 ("건수" / "비율%") |

### 9.4 발송 워크플로우 개선

| 개선 | 현재 | 제안 |
|------|------|------|
| 탭 구조 | 3개 탭 (탭 이동 시 폼 초기화) | 단일 폼 + 라디오 전환 |
| 발송 후 | Result 성공 화면만 | 배치 진행률 + 재발송 버튼 |
| 발송 전 확인 | 없음 | 수신자 수 + 쿼터 잔여량 요약 |
| 대량 수신자 | 태그 입력만 | CSV/Excel 업로드 지원 |

### 9.5 구현 우선순위

| 우선순위 | 개선 | 공수 |
|---------|------|------|
| 즉시 | 메뉴 "테스트 이메일 발송" → "이메일 발송" | 1줄 |
| 즉시 | 도넛 차트 중앙 총계 | 10줄 |
| 즉시 | 상태 색상 상수 통합 | 1파일 |
| 단기 | 글로벌 알림 시스템 | 2~3일 |
| 단기 | 모니터링 자동 갱신 (60초) | 1줄 |
| 중기 | SendEmail 폼 개편 | 2~3일 |
| 중기 | 배치 진행률 표시 | 1~2일 |
| 장기 | 인라인 스타일 → CSS 토큰 | 3~5일 |
