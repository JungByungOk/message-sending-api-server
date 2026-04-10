# Project Overview

## ESM (Email Sending Management)

멀티테넌트 이메일 발송 관리 플랫폼

### 목적

다양한 고객사(테넌트)가 동일한 AWS SES 인프라를 공유하면서 각자의 도메인/이메일로 독립적으로 이메일을 발송할 수 있도록 통합 관리합니다. AWS SDK를 직접 사용하지 않고 API Gateway를 단일 진입점으로 사용하여 보안과 운영을 단순화합니다.

### 시스템 구성

```
┌───────────────────────────────────────────────────────────────┐
│              Client / Frontend (React 19 + Vite)               │
└──────────────────────────┬────────────────────────────────────┘
                           │ REST API (Tenant API Key Auth)
┌──────────────────────────▼────────────────────────────────────┐
│                  ESM Backend (Spring Boot :7092)                │
│                                                                │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌────────────────┐  │
│  │ SES      │ │ Tenant   │ │ Settings  │ │ Suppression    │  │
│  │ Module   │ │ Module   │ │ Module    │ │ Module         │  │
│  └──────────┘ └──────────┘ └───────────┘ └────────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌────────────────┐  │
│  │Onboarding│ │Monitoring│ │ Scheduler │ │ SES Identity  │  │
│  │ Module   │ │ Module   │ │ Module    │ │ / ConfigSet    │  │
│  └──────────┘ └──────────┘ └───────────┘ └────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ ResultPollingService (DynamoDB 폴링 → 로컬 DB 보정)    │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                                │
│  [Spring Security]                                             │
│   - JwtAuthenticationFilter (JWT Bearer 토큰 검증)              │
│   - ApiKeyAuthenticationFilter (Tenant API Key / 레거시 Key)    │
│   - TenantContextFilter (ThreadLocal 정리)                     │
└──────────────┬─────────────────────┬──────────────────────────┘
               │                     │
    ┌──────────▼──────────┐ ┌────────▼──────────────────────────┐
    │    PostgreSQL        │ │    AWS API Gateway                 │
    │  (esm / esm / esm)  │ │  (IP Whitelist + API Key 인증)    │
    └─────────────────────┘ └──┬────────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────────┐
            │                  │                      │
     ┌──────▼──────┐                         ┌─────────▼────────┐
     │     SQS     │                         │  Lambda          │
     │ems-send-q   │                         │  ems-event-query │
     │(+DLQ)       │                         └─────────┬────────┘
     └──────┬──────┘                                   │
            │                                   ┌──────▼────────┐
     ┌──────▼──────┐                            │   DynamoDB    │
     │   Lambda    │                            │ems-send-      │
     │email-sender │                            │results        │
     └──────┬──────┘                            └──────────────┘
            │    correlationId                         ▲
            │    in EmailTag                           │
     ┌──────▼──────┐                                   │
     │  Amazon SES │                                   │
     │             │                                   │
     └──────┬──────┘          (ResultPollingService 2분 주기)
            │ EventBridge
            └──────────────────────────────▶ Lambda event-processor
                                                       │
                                            ┌──────────▼──────────┐
                                            │  Lambda suppression  │
                                            │  (Bounce/Complaint)  │
                                            └─────────────────────┘
```

### 모듈 설명

| Module | Description | 외부 연동 |
|--------|-------------|-----------|
| SES Module | 이메일/템플릿 발송 요청, correlationId 생성 및 EmailTag 주입 | AWS API Gateway → SQS → Lambda → SES |
| Tenant Module | 멀티테넌트 고객사 관리, API Key 발급, 할당량, 발신자 이메일 관리 | PostgreSQL |
| Scheduler Module | Quartz 기반 주기 작업 (폴링/타임아웃/동기화만 담당, 배치 발송은 SQS로 이관) | PostgreSQL |
| EmailDispatch Module | 이메일 발송 큐 등록 (할당량 검증 → API GW /email-enqueue → SQS) | API Gateway |
| ResultPollingService | DynamoDB 발송결과 주기 폴링(2분), correlationId 매칭, 이벤트 이력 기록 | AWS API Gateway → Lambda → DynamoDB |
| Onboarding Module | 테넌트 온보딩 (생성→도메인/이메일 인증→활성화) | AWS API Gateway |
| Suppression Module | 수신 거부(Bounce/Complaint) 목록 관리 | PostgreSQL |
| SES Identity Module | SES 도메인 아이덴티티 등록 및 DKIM 관리 | AWS API Gateway |
| SES ConfigSet Module | 테넌트별 SES 구성 세트 관리 | AWS API Gateway |
| Auth Module | JWT 인증, 사용자 관리, 비밀번호 변경 | - |
| Monitoring Module | 발송 통계, 트렌드, 테넌트 평판 모니터링 (MonitoringService) | PostgreSQL |
| Cost Estimate Module | AWS 서비스별 월별 비용 추정 (CostEstimateService) | PostgreSQL |
| Settings Module | API Gateway 연결 설정, 폴링 주기 관리 (1~10분 설정 가능), VDM ON/OFF | PostgreSQL |

### 기술 스택

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.1, Java 17 |
| Frontend | React 19.2, Vite 8, TypeScript 5.9, Ant Design 5.29 |
| Database | PostgreSQL (esm/esm/esm) |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz 2.5.0 (PostgreSQL DB Store) |
| AWS 연동 | API Gateway 경유 (Java HTTP Client) — AWS SDK 미사용 |
| AWS 인프라 | CDK v2 자동 구축 (Lambda 7개, DynamoDB 3개, SQS, EventBridge, S3) |
| API Docs | SpringDoc OpenAPI 2.7.0 (Swagger UI) |
| Security | Spring Security (JWT + API Key 필터) |
| Build | Gradle, Docker (Multi-stage) |
| Monitoring | Spring Actuator |

### 데이터 흐름

#### 이메일 발송 흐름

```
1. [ESM] EmailDispatchService → 할당량 검증 (QuotaService) + DB 발송 이력 기록
2. [ESM] EmailDispatchService → API Gateway POST /email-enqueue (ApiGatewayClient)
3. [Lambda ems-enqueue] → SQS ems-send-queue 등록 (비동기 큐잉)
4. [Lambda ems-email-sender] → SQS 트리거 → DynamoDB ems-idempotency 중복 확인
5. [Lambda ems-email-sender] → DynamoDB ems-tenant-config (Config Set 조회)
6. [Lambda ems-email-sender] → Amazon SES 이메일 발송
```

#### 발송 결과 수신 흐름 (EventBridge → DynamoDB → Polling)

```
[이벤트 수신 - EventBridge]
1. [SES] → EventBridge Bus ems-ses-events → Lambda ems-event-processor
   (9가지 이벤트: SEND/DELIVERY/BOUNCE/COMPLAINT/OPEN/CLICK/REJECT/DELIVERY_DELAY/RENDERING_FAILURE)
2. [Lambda ems-event-processor] → SES EmailTag에서 correlationId 추출 → DynamoDB ems-send-results 저장
3. [EventBridge] → Lambda ems-suppression (BOUNCE/COMPLAINT 이벤트)
   → DynamoDB suppression 테이블 자동 등록

[보정 폴링 - ResultPollingService]
1. [ESM ResultPollingService] → API Gateway GET /results?tenant_id=X&after=T (2분 주기)
2. [Lambda ems-event-query] → DynamoDB ems-send-results GSI Query
3. [ESM] → correlationId로 Terminal 상태 보호 조건부 UPDATE (멱등)
   - DELIVERY        → Delivered
   - BOUNCE          → Bounced + 수신거부 목록 자동 등록
   - COMPLAINT       → Complained + 수신거부 목록 자동 등록
   - OPEN / CLICK    → Delivered 유지 (Engagement 이벤트, send_rslt_typ_cd만 업데이트)
   - REJECT          → Rejected
   - DELIVERY_DELAY  → Delayed
   - RENDERING_FAILURE → Error
4. [ESM] → ADM_EMAIL_EVENT_LOG 이벤트 이력 기록
```

#### Correlation ID 기반 이메일 추적

```
[발송 시점 - 3경로 모두 동일]
Backend (EmailController / SendTemplatedEmailJob / SendTemplatedEmailWithPollingJob)
  → UUID 생성 → correlation_id 로컬 DB 저장
  → SES EmailTag: { name: "correlation_id", value: "<uuid>" } 주입
  → API Gateway → Lambda email-sender → SES 발송

[결과 수신 시점]
SES EmailTag → event-processor 추출 → correlationId DynamoDB에 포함
  → ResultPollingService
  → WHERE correlation_id = ? 로 ADM_EMAIL_SEND_DTL 1-hop 매칭
  → ses_message_id 별도 컬럼에 저장
```

- **배경**: SQS Message ID ≠ SES Message ID 불일치로 기존 매칭 불가
- **해결**: Backend가 UUID를 생성하여 SES EmailTag로 전달하는 1-hop 추적 체계

#### 설정 동기화 흐름

```
1. [ESM 설정 UI] → PUT /settings/aws (ESM DB SYSTEM_CONFIG 저장)
2. [ESM] → API Gateway PUT /config (설정 동기화)
```

#### 온보딩 흐름

```
[도메인 인증 방식]
1. POST /onboarding/start → 테넌트 생성 + API Gateway POST /tenant-setup (SES Identity 등록)
2. 관리자 → DNS에 DKIM CNAME 레코드 추가
3. GET /onboarding/{id}/dkim → 인증 상태 확인 (SUCCESS/PENDING)
4. POST /onboarding/{id}/activate → ConfigSet 생성 + 테넌트 ACTIVE 전환

[이메일 개별 인증 방식 - DNS 접근 불가 시]
1. POST /onboarding/{id}/verify-email → SES 이메일 아이덴티티 등록
2. SES가 해당 이메일 주소로 인증 메일 자동 발송
3. 수신자 인증 링크 클릭 → 인증 완료
4. GET /onboarding/{id}/email-status/{email} → 인증 상태 확인
```

### 발신자 이메일 관리 (TENANT_SENDER)

테넌트별로 이메일 발송 시 사용할 수 있는 발신자 이메일 주소를 관리합니다.

- 발신자 이메일은 테넌트에 등록된 도메인과 일치해야 합니다 (도메인 제한)
- 이메일 발송 요청 시 `SenderValidationService`가 `from` 주소를 TENANT_SENDER 테이블에서 검증
- 온보딩 완료 후 도메인 인증이 끝난 이메일만 등록 가능

### 배포 환경

| Environment | Profile | Description |
|-------------|---------|-------------|
| Local | `local` | 로컬 개발 (Docker Compose PostgreSQL) |
| Development | `dev` | 개발 서버 (`/svc/ems/config/ems-config-dev.yml` 오버라이드) |
| Production | `prod` | 운영 서버 (`/svc/ems/config/ems-config-prod.yml` 오버라이드) |

### AWS CDK 인프라 (`aws/ems-cdk-v2/`)

> `aws/ems-cdk-v2/`가 `aws/ems-cdk/`를 대체합니다. `cdk deploy` 한 번으로 아래 모든 리소스가 자동 생성됩니다.

| 리소스 | 이름 | 용도 |
|--------|------|------|
| API Gateway | ems-api | ESM ↔ AWS 단일 진입점 |
| Lambda | ems-email-sender | SQS 트리거 → SES 발송 |
| Lambda | ems-enqueue | API GW → SQS 발송 큐 등록 |
| Lambda | ems-event-processor | EventBridge 트리거 → DynamoDB 저장 |
| Lambda | ems-event-query | DynamoDB 발송결과 조회 (보정 폴링) |
| Lambda | ems-suppression | EventBridge → Bounce/Complaint 처리 |
| Lambda | ems-tenant-setup | Identity/ConfigSet/템플릿 CRUD, `GET_ACCOUNT` 액션(SES 계정 정보 조회), `CLEAR_EVENTS` 액션(DynamoDB ems-send-results + ems-idempotency 초기화) |
| Lambda | ems-tenant-sync | EventBridge → 테넌트 상태 동기화 |
| SQS | ems-send-queue (+DLQ) | 비동기 발송 큐 |
| EventBridge Bus | ems-ses-events | SES 이벤트 수신 버스 |
| EventBridge Archive | ems-ses-events-archive | 이벤트 아카이브 (30일 보관) |
| DynamoDB | ems-send-results | 발송결과 저장 (TTL 7일) |
| DynamoDB | ems-tenant-config | 테넌트 Config Set 캐시 (TTL 1시간) |
| DynamoDB | ems-idempotency | 중복발송 방지 (TTL 24시간) |
| S3 | ems-bulk-recipients | 대량 발송 수신자 목록 저장 |

### 모니터링 API

| Endpoint | 설명 |
|----------|------|
| `GET /monitoring/ses-quota` | SES 일간 발송 한도 조회 — Lambda `tenant-setup GET_ACCOUNT` 액션으로 MaxSendRate, Max24HourSend, SentLast24Hours 반환. Frontend 대시보드 "이메일 발송 일간 한도" 카드에 사용률 Progress bar + 잔여 건수 표시 |
| `GET /monitoring/tenant-metrics/{tenantId}` | CloudWatch 테넌트별 SES 메트릭 조회 (period 파라미터, 기본 3600초) |
| `GET /monitoring/cost/real` | Cost Explorer 실 비용 조회 (startDate, endDate 파라미터). 실패 시 추정치 폴백. |

### PostgreSQL 주요 테이블

| 테이블 | 설명 |
|--------|------|
| `ADM_EMAIL_SEND_MST` | 이메일 발송 마스터 (1건 발송 요청 단위) |
| `ADM_EMAIL_SEND_DTL` | 이메일 발송 상세 (수신자 1명 단위, correlation_id + ses_message_id 컬럼 포함) |
| `ADM_EMAIL_SEND_BATCH` | 예약 발송 배치 메타 (batchId, tenantId, 발송 건수 등) — JobDataMap 크기 제한 해소 |
| `ADM_EMAIL_SEND_BATCH_ITEM` | 예약 발송 배치 수신자 목록 (ADM_EMAIL_SEND_BATCH 1:N) |
| `ADM_EMAIL_EVENT_LOG` | SES 이벤트 이력 (Delivery, Bounce, Open, Click 등 모든 이벤트 기록) |
| `ADM_EMAIL_BL_MST` | 수신 거부/블랙리스트 목록 |
| `TENANT` | 테넌트 정보 및 API Key |
| `TENANT_SENDER` | 테넌트별 허용 발신자 이메일 목록 |
| `ADM_TEMPLATE_TENANT_MAP` | 테넌트별 템플릿 매핑 (SUBJECT 컬럼 포함 — 예약 발송 결과에 실제 제목 표시) |
| `ADM_USER_MST` | 관리자 계정 (JWT 인증용, BCrypt 해시 비밀번호) |
| `SYSTEM_CONFIG` | API Gateway 설정, 폴링 주기 등 |
