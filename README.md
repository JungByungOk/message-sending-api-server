# Joins EMS (Email Management System)

멀티테넌트 이메일 발송 관리 플랫폼 — AWS SES 네이티브 기능을 최대한 활용하는 이메일 발송, 테넌트 관리, 모니터링 시스템

> **모든 AWS 연동은 API Gateway를 경유합니다.** AWS SDK 직접 연결 없이 `ApiGatewayClient`(Java HTTP Client)로 API Gateway를 호출합니다.

## Project Structure

```
message-sending-api-server/
├── backend/                # Spring Boot API 서버 (Java 17)
├── frontend/               # React 19 어드민 대시보드 (Vite + TypeScript)
├── aws/
│   └── ems-cdk-v2/         # AWS CDK v2 인프라 코드 (EventBridge, Lambda, SQS, DynamoDB)
├── docs/                   # 프로젝트 문서
├── docker-compose.yml      # 로컬 개발용 PostgreSQL
└── README.md
```

## Documentation

| Document | Description |
|----------|-------------|
| [Project Overview](docs/project-overview.md) | 프로젝트 개요, 시스템 구성, 데이터 흐름 |
| [Common Feature Spec](docs/common-feature-spec.md) | 공통 기능 명세 (인증, 에러 처리, 상태 코드) |
| [Backend API Spec](docs/backend-spec.md) | Backend REST API 상세 명세서 |
| [Frontend Spec](docs/frontend-spec.md) | Frontend 기술 명세 (타입, 상태 관리, 라우팅) |
| [Deployment Guide](docs/deployment-guide.md) | 배포 절차 가이드 (CDK → Backend → Frontend) |
| [Migration Design](docs/v2/ses-native-migration.md) | SES 네이티브 마이그레이션 설계 문서 |

## Tech Stack

### Backend

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.4.1 |
| Language | Java 17 |
| Database | PostgreSQL 16 |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz 2.5.0 (폴링/타임아웃/동기화만 담당) |
| AWS 연동 | API Gateway 경유 (Java HTTP Client) |
| Security | Spring Security (JWT + Tenant API Key) |
| Auth | jjwt 0.12.6 (Access/Refresh Token) |
| API Docs | SpringDoc OpenAPI 2.7.0 (Swagger UI) |
| Build | Gradle 9.4, Docker (Multi-stage) |

### Frontend

| Category | Technology |
|----------|-----------|
| Framework | React 19.2 |
| Language | TypeScript 5.9 |
| Build | Vite 7 |
| UI | Ant Design 5.29, @ant-design/pro-components |
| Server State | TanStack React Query 5 |
| Client State | Zustand 5 |
| Routing | React Router DOM 7 |
| HTTP Client | Axios (JWT 자동 갱신 인터셉터) |

### AWS Infrastructure (CDK v2)

| Resource | Name | Purpose |
|----------|------|---------|
| API Gateway | ems-api-v2 | Backend ↔ AWS 단일 진입점 |
| EventBridge | ems-ses-events | SES 이벤트 라우팅 (SNS 대체) |
| Lambda | ems-email-sender | SQS 트리거 → SES 발송 (TenantName) |
| Lambda | ems-enqueue | API GW → 수신자별 SQS 메시지 생성 |
| Lambda | ems-event-processor | EventBridge → Delivery 이벤트 처리 |
| Lambda | ems-suppression | EventBridge → Bounce/Complaint 처리 |
| Lambda | ems-tenant-setup | SES Identity/ConfigSet/Template/VDM 관리 |
| Lambda | ems-tenant-sync | EventBridge → 테넌트 상태 동기화 |
| Lambda | ems-event-query | DynamoDB 발송결과 조회 |
| SQS | ems-send-queue + DLQ | 비동기 발송 큐 (BatchSize 10) |
| DynamoDB | ems-send-results | 발송결과 (TTL 7일) |
| DynamoDB | ems-tenant-config | 테넌트 설정 캐시 |
| DynamoDB | ems-suppression | 수신거부 동기화 |
| S3 | ems-batch-* | 대량 발송 수신자 목록 (>1,000건) |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│         Frontend (React 19 + Vite + TypeScript)              │
│         JWT 인증 / 30분 비활동 자동 로그아웃                   │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST API (JWT / Tenant API Key)
┌──────────────────────────▼──────────────────────────────────┐
│                 ESM Backend (Spring Boot :7092)               │
│                                                              │
│  Auth | Tenant | EmailDispatch | Settings | Monitoring       │
│  Onboarding | Suppression | Scheduler | SES Identity/Config  │
│                                                              │
│  [Spring Security]                                           │
│   - JwtAuthenticationFilter (JWT Bearer 토큰)                │
│   - ApiKeyAuthenticationFilter (Tenant API Key)              │
│   - TenantContextFilter (ThreadLocal 정리)                   │
└──────────────┬──────────────────────┬───────────────────────┘
               │                      │
    ┌──────────▼──────────┐  ┌────────▼────────────────────────┐
    │    PostgreSQL 16     │  │    AWS API Gateway (ems-api-v2) │
    │  (ems / ems)         │  │    IP Whitelist + API Key 인증  │
    └─────────────────────┘  └──┬──────────────────────────────┘
                                │
         ┌──────────────────────┼──────────────────────┐
         │                      │                      │
  ┌──────▼──────┐  ┌────────────▼────────┐  ┌─────────▼────────┐
  │  Lambda     │  │  Lambda             │  │  Lambda           │
  │  enqueue    │  │  tenant-setup       │  │  event-query      │
  └──────┬──────┘  └─────────────────────┘  └─────────┬────────┘
         │                                            │
  ┌──────▼──────┐                              ┌──────▼────────┐
  │  SQS Queue  │                              │   DynamoDB    │
  │  (+DLQ)     │                              │   3 tables    │
  └──────┬──────┘                              └───────────────┘
         │                                            ▲
  ┌──────▼──────┐                                     │
  │  Lambda     │──────────▶ Amazon SES ──────▶ EventBridge
  │email-sender │           (TenantName)        │
  └─────────────┘                          ┌────┴────────────┐
                                           │                 │
                                    ┌──────▼─────┐  ┌───────▼───────┐
                                    │ Lambda     │  │ Lambda        │
                                    │ event-     │  │ suppression   │
                                    │ processor  │  │ (Bounce/      │
                                    │            │  │  Complaint)   │
                                    └────────────┘  └───────────────┘
```

## API Endpoints

### Auth — 인증

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/login` | 로그인 (JWT 발급) |
| `POST` | `/auth/refresh` | 토큰 갱신 |
| `POST` | `/auth/change-password` | 비밀번호 변경 |
| `GET` | `/users/me` | 내 정보 조회 |
| `GET` | `/users` | 사용자 목록 |
| `POST` | `/users` | 사용자 생성 |

### Email — 이메일 발송

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/text-mail` | HTML 이메일 발송 (EmailDispatchService → SQS) |
| `POST` | `/ses/templated-mail` | 템플릿 이메일 발송 (EmailDispatchService → SQS) |
| `POST` | `/ses/template` | 이메일 템플릿 생성 |
| `PATCH` | `/ses/template` | 이메일 템플릿 수정 |
| `DELETE` | `/ses/template` | 이메일 템플릿 삭제 |
| `GET` | `/ses/templates` | 템플릿 목록 조회 |

### Tenant — 테넌트 관리

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/tenant` | 테넌트 등록 (SES Identity + ConfigSet 자동 생성) |
| `GET` | `/tenant/{id}` | 테넌트 조회 |
| `GET` | `/tenant/list` | 테넌트 목록 (페이징) |
| `PATCH` | `/tenant/{id}` | 테넌트 수정 |
| `DELETE` | `/tenant/{id}` | 테넌트 비활성화 |
| `DELETE` | `/tenant/{id}/permanent` | 테넌트 영구 삭제 (SES 리소스 정리) |
| `POST` | `/tenant/{id}/activate` | 테넌트 활성화 |
| `POST` | `/tenant/{id}/pause` | 발송 일시정지 |
| `POST` | `/tenant/{id}/resume` | 발송 재개 |
| `POST` | `/tenant/{id}/regenerate-key` | API Key 재발급 |
| `GET` | `/tenant/{id}/quota` | 할당량 사용 현황 |
| `PATCH` | `/tenant/{id}/quota` | 할당량 수정 |
| `GET` | `/tenant/{id}/senders` | 발신자 이메일 목록 |
| `POST` | `/tenant/{id}/senders` | 발신자 등록 |
| `DELETE` | `/tenant/{id}/senders/{email}` | 발신자 삭제 |

### Monitoring — 모니터링

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/monitoring/summary` | 대시보드 요약 통계 |
| `GET` | `/monitoring/hourly` | 시간대별 발송량 |
| `GET` | `/monitoring/status-summary` | 상태별 발송 건수 |
| `GET` | `/monitoring/trend` | 주간/월간 트렌드 |
| `GET` | `/monitoring/ses-quota` | SES 일간 발송 한도 |
| `GET` | `/monitoring/tenant-metrics/{id}` | CloudWatch 테넌트 메트릭 |
| `GET` | `/monitoring/cost` | 월별 추정 비용 |
| `GET` | `/monitoring/cost/real` | Cost Explorer 실 비용 |
| `GET` | `/monitoring/tenant-reputation` | 테넌트별 평판 |

### Settings — 설정

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/settings/aws` | AWS 설정 조회 |
| `PUT` | `/settings/aws` | AWS 설정 저장 |
| `POST` | `/settings/aws/test` | API Gateway 연결 테스트 |
| `GET` | `/settings/vdm` | VDM 상태 조회 |
| `PUT` | `/settings/vdm` | VDM ON/OFF |
| `GET` | `/settings/polling-interval` | 폴링 주기 조회 |
| `PUT` | `/settings/polling-interval` | 폴링 주기 변경 (1~10분) |

### Onboarding / Suppression / SES Identity / ConfigSet / Scheduler

> 상세 API는 [Backend API Spec](docs/backend-spec.md) 참조

## Key Features

### 이중 인증 (JWT + API Key)
- **관리자 대시보드**: JWT 인증 (Access 30분, Refresh 7일, 30분 비활동 자동 로그아웃)
- **외부 테넌트**: API Key 인증 (발송 API만 접근 가능)
- 초기 계정: `admin` / `admin`

### SES 네이티브 발송 파이프라인
- `EmailDispatchService` → API Gateway `/email-enqueue` → Lambda → SQS → Lambda → SES
- 소량(≤1,000건): body에 수신자 포함 / 대량(>1,000건): S3 참조
- SQS BatchSize 10, DLQ 자동 재처리
- SES TenantName으로 테넌트별 평판 격리

### EventBridge 이벤트 라우팅
- Rule 1: Bounce/Complaint → Lambda suppression (자동 수신거부 등록)
- Rule 2: Delivery/Open/Click/... → Lambda event-processor (DynamoDB 저장)
- Rule 3: 테넌트 상태 변화 → Lambda tenant-sync
- Archive: 30일 보관, 장애 시 리플레이

### 테넌트 SES 동기화
- 테넌트 생성 시 SES Identity + ConfigSet 자동 생성
- 테넌트 삭제 시 SES 리소스 자동 정리
- 일시정지/재개 API

### 2계층 할당량 관리
- AWS 계정 한도: SES `GetAccount` (실시간)
- 테넌트별 한도: `QuotaService` (일별/월별, Backend 관리)

### 모니터링
- CloudWatch 테넌트별 SES 메트릭 (5분 캐시)
- Cost Explorer 실 비용 조회 (1일 캐시, 추정치 폴백)
- VDM ON/OFF 토글 (ISP별 전달률 인사이트)

### 통일된 응답 형식
- `ApiResponse<T>` 래퍼: `{ success, data, error: { code, message, details } }`
- Bean Validation + GlobalControllerAdvice

## Getting Started

### Prerequisites
- Java 17, Docker, Node.js 20+
- AWS CLI + CDK CLI (`npm install -g aws-cdk`)

### 1. 로컬 DB 시작

```bash
docker compose up -d
# 초기 스키마는 docker-entrypoint-initdb.d로 자동 실행
```

### 2. CDK v2 배포

```bash
cd aws/ems-cdk-v2
npm install
cdk bootstrap   # 최초 1회
cdk deploy --context esmServerIp="서버IP/32"
```

### 3. Backend 실행

```bash
cd backend
# .env 파일 설정 (DB_URL, JWT_SECRET 등)
./gradlew bootRun
```

### 4. Frontend 실행

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173
```

### 5. 초기 로그인

`http://localhost:5173` → `admin` / `admin` → 비밀번호 변경 → AWS 설정 입력

> 상세 배포 절차는 [Deployment Guide](docs/deployment-guide.md) 참조

### 주요 URL

| URL | Description |
|-----|-------------|
| http://localhost:7092/swagger-ui.html | Swagger UI |
| http://localhost:7092/actuator/health | Health Check |
| http://localhost:5173 | Admin Dashboard |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/ems` |
| `DB_USERNAME` | DB 사용자명 | `ems` |
| `DB_PASSWORD` | DB 비밀번호 | `ems1234` |
| `JWT_SECRET` | JWT 서명 키 (256-bit 이상) | 내장 기본값 |
| `QUARTZ_DB_URL` | Quartz DB URL | `jdbc:postgresql://localhost:5432/ems` |
| `QUARTZ_DB_USER` | Quartz DB 사용자 | `ems` |
| `QUARTZ_DB_PASSWORD` | Quartz DB 비밀번호 | `ems1234` |

## Backend Module Structure

```
backend/src/main/java/com/msas/
├── auth/                   # JWT 인증, 사용자 관리
├── common/
│   ├── dto/                # ApiResponse<T> 공통 응답 래퍼
│   ├── exceptionhandler/   # GlobalControllerAdvice
│   ├── httplog/            # HTTP 로깅 인터셉터
│   ├── security/           # JWT + API Key 필터, SecurityConfig
│   └── tenant/             # TenantContext (ThreadLocal)
├── monitoring/             # MonitoringService, CostEstimateService
├── onboarding/             # 테넌트 온보딩 (도메인/이메일 인증)
├── pollingchecker/         # 보정 폴링 (DynamoDB 결과 조회, 2분 주기)
├── scheduler/              # Quartz (폴링/타임아웃/동기화만 담당)
├── ses/
│   ├── controller/         # EmailController (발송, 템플릿)
│   ├── service/            # EmailDispatchService (SQS 파이프라인)
│   ├── configset/          # SES ConfigSet 관리
│   └── identity/           # SES 도메인 아이덴티티
├── settings/               # API Gateway 설정, VDM, 폴링 주기
├── suppression/            # 수신 거부 관리
└── tenant/                 # 테넌트 CRUD, SES 동기화, 할당량
```
