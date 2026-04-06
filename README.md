# Joins ESM (Email Sending Management)

멀티테넌트 이메일 발송 관리 플랫폼 - AWS SES 기반 이메일 발송, 테넌트 온보딩, 수신 거부 관리를 제공하는 Spring Boot + React 풀스택 SaaS 서버

> **모든 AWS 연동은 API Gateway를 경유합니다.** AWS SDK 직접 연결 없이 `ApiGatewayClient`(Java HTTP Client)로 API Gateway를 호출합니다.

## Project Structure

```
message-sending-api-server/
├── backend/                # Spring Boot API 서버 (Java 17)
│   ├── src/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── start.bat
├── frontend/               # React 19 어드민 대시보드 (Vite + TypeScript)
├── aws/
│   └── ems-cdk/            # AWS CDK 인프라 코드 (Lambda, DynamoDB, SQS, SNS, SSM)
├── docs/                   # 프로젝트 문서
└── README.md
```

## Documentation

| Document | Description |
|----------|-------------|
| [Project Overview](docs/project-overview.md) | 프로젝트 개요, 시스템 구성, 데이터 흐름 |
| [Common Feature Spec](docs/common-feature-spec.md) | 공통 기능 명세 (인증, 에러 처리, 상태 코드 등) |
| [Backend API Spec](docs/backend-spec.md) | Backend REST API 상세 명세서 |
| [Frontend Spec](docs/frontend-spec.md) | Frontend 기술 명세 (타입, 상태 관리, 라우팅) |
| [Frontend Development Plan](docs/frontend-development-plan.md) | Frontend 개발 계획 (Phase별) |
| [CDK Infrastructure](aws/ems-cdk/README.md) | AWS CDK 인프라 구축 가이드 |

## Tech Stack

### Backend

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.4.1 |
| Language | Java 17 |
| Database | PostgreSQL (esm/esm/esm) |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz (PostgreSQL DB Store) |
| AWS 연동 | API Gateway 경유 (Java HTTP Client, `ApiGatewayClient`) |
| Security | Spring Security (Tenant API Key + Callback Secret 검증) |
| API Docs | SpringDoc OpenAPI 2.7.0 (Swagger UI) |
| Build | Gradle, Docker (Multi-stage) |
| Etc | Guava (RateLimiter), Gson 2.11, Lombok |

### Frontend

| Category | Technology |
|----------|-----------|
| Framework | React 19.2 |
| Language | TypeScript 5.9 |
| Build | Vite 8 |
| UI Components | Ant Design 5.29, @ant-design/pro-components 2.8 |
| Icons | @ant-design/icons 6.1 |
| Server State | TanStack React Query 5 |
| Client State | Zustand 5 |
| Routing | React Router DOM 7 |
| HTTP Client | Axios 1.14 |
| Date | Day.js 1.11 |
| Lint | ESLint 9 |

### AWS Infrastructure (CDK)

| Resource | Name | Purpose |
|----------|------|---------|
| API Gateway | ems-api | ESM → AWS 단일 진입점 |
| Lambda | ems-email-sender | SQS → SES 이메일 발송 |
| Lambda | ems-event-processor | SNS → DynamoDB 저장 + ESM 콜백 |
| Lambda | ems-event-query | DynamoDB 발송결과 조회 |
| Lambda | ems-tenant-setup | Identity/ConfigSet/템플릿 관리 |
| Lambda | ems-config-updater | SSM Parameter Store 설정 업데이트 |
| SQS | ems-send-queue (+DLQ) | 비동기 발송 큐 |
| SNS | ems-ses-events | SES 이벤트 수신 |
| DynamoDB | ems-send-results | 발송결과 (TTL 7일) |
| DynamoDB | ems-tenant-config | 테넌트 설정 캐시 (TTL 1시간) |
| DynamoDB | ems-idempotency | 중복발송 방지 (TTL 24시간) |
| SSM | /ems/mode, /ems/callback_url, /ems/callback_secret | 수신 모드 설정 |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│           Frontend (React 19 + Vite + TypeScript)        │
└────────────────────────┬────────────────────────────────┘
                         │ REST API (Tenant API Key)
┌────────────────────────▼────────────────────────────────┐
│                  ESM Backend (Spring Boot :7092)          │
│                                                          │
│  SES | Tenant | Callback | Settings | Onboarding         │
│  Suppression | Scheduler | SES Identity | SES ConfigSet  │
│                                                          │
│  [Spring Security]                                       │
│   - Tenant API Key 인증 (Authorization 헤더)             │
│   - Callback Secret 검증 (X-Callback-Secret 헤더)        │
└──────────────┬─────────────────────┬────────────────────┘
               │                     │
    ┌──────────▼──────────┐ ┌────────▼───────────────────┐
    │    PostgreSQL        │ │    AWS API Gateway          │
    │  (esm/esm/esm)       │ │  (IP Whitelist / API Key)  │
    └─────────────────────┘ └──┬─────────────────────────┘
                               │
            ┌──────────────────┼─────────────────────┐
            │                  │                     │
     ┌──────▼──────┐  ┌────────▼────────┐  ┌────────▼────────┐
     │     SQS     │  │   SSM Parameter │  │ Lambda          │
     │ ems-send-q  │  │   Store         │  │ ems-event-query │
     └──────┬──────┘  └────────┬────────┘  └────────┬────────┘
            │                  │ (30초 캐시)          │
     ┌──────▼──────┐  ┌────────▼────────┐  ┌────────▼────────┐
     │   Lambda    │  │ Lambda          │  │    DynamoDB     │
     │email-sender │  │ event-processor │  │ ems-send-results│
     └──────┬──────┘  └────────┬────────┘  └─────────────────┘
            │                  │ callback 모드               ▲
     ┌──────▼──────┐    ┌──────▼──────┐                     │
     │  Amazon SES │───▶│ESM /ses/    │                     │
     │  (발송)     │    │callback/    │                     │
     └─────────────┘    │event        │─────────────────────┘
          │ SNS          └─────────────┘  (보정 폴링: 5~10분)
          └─────────────────────────────▶ Lambda event-processor
```

## API Endpoints

### AWS SES - 이메일 발송 (API Gateway 경유)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/text-mail` | HTML 이메일 발송 |
| `POST` | `/ses/templated-mail` | 템플릿 기반 이메일 발송 |
| `POST` | `/ses/template` | 이메일 템플릿 생성 |
| `PATCH` | `/ses/template` | 이메일 템플릿 수정 |
| `DELETE` | `/ses/template` | 이메일 템플릿 삭제 |
| `GET` | `/ses/templates` | 템플릿 목록 조회 |

### Tenant - 테넌트 관리

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/tenant` | 테넌트 등록 |
| `GET` | `/tenant/{tenantId}` | 테넌트 조회 |
| `GET` | `/tenant/list` | 테넌트 목록 조회 (페이징) |
| `PATCH` | `/tenant/{tenantId}` | 테넌트 수정 |
| `DELETE` | `/tenant/{tenantId}` | 테넌트 비활성화 |
| `DELETE` | `/tenant/{tenantId}/permanent` | 테넌트 영구 삭제 |
| `POST` | `/tenant/{tenantId}/activate` | 테넌트 활성화 |
| `POST` | `/tenant/{tenantId}/regenerate-key` | API Key 재발급 |
| `GET` | `/tenant/{tenantId}/quota` | 할당량 사용 현황 |
| `PATCH` | `/tenant/{tenantId}/quota` | 할당량 수정 |
| `GET` | `/tenant/{tenantId}/senders` | 발신자 이메일 목록 조회 |
| `POST` | `/tenant/{tenantId}/senders` | 발신자 이메일 등록 |
| `DELETE` | `/tenant/{tenantId}/senders/{email}` | 발신자 이메일 삭제 |

### Onboarding - 테넌트 온보딩

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/onboarding/start` | 온보딩 시작 (테넌트 생성 + SES Identity 등록) |
| `GET` | `/onboarding/{tenantId}/status` | 온보딩 상태 조회 |
| `GET` | `/onboarding/{tenantId}/dkim` | DKIM 레코드 조회 |
| `POST` | `/onboarding/{tenantId}/activate` | 테넌트 수동 활성화 (ConfigSet 구성 포함) |
| `POST` | `/onboarding/{tenantId}/verify-email` | 이메일 개별 인증 요청 |
| `GET` | `/onboarding/{tenantId}/email-status/{email}` | 이메일 인증 상태 조회 |
| `POST` | `/onboarding/{tenantId}/resend-verification/{email}` | 인증 이메일 재발송 |

### SES Callback - 이벤트 콜백 (X-Callback-Secret 검증)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/callback/event` | SES 이벤트 처리 (DELIVERY/BOUNCE/COMPLAINT) |
| `GET` | `/ses/callback/health` | 콜백 상태 확인 |

### SES Identity - 도메인 아이덴티티 (API Gateway 경유)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/identity` | 도메인 아이덴티티 생성 (DKIM 등록) |
| `GET` | `/ses/identity/{domain}` | 도메인 인증 상태 조회 |
| `DELETE` | `/ses/identity/{domain}` | 도메인 아이덴티티 삭제 |

### SES ConfigSet - 구성 세트 (API Gateway 경유)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/config-set` | ConfigSet 생성 |
| `GET` | `/ses/config-set/{tenantId}` | ConfigSet 조회 |
| `DELETE` | `/ses/config-set/{tenantId}` | ConfigSet 삭제 |

### Suppression - 수신 거부

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/suppression/tenant/{tenantId}` | 수신 거부 목록 조회 |
| `DELETE` | `/suppression/tenant/{tenantId}/{email}` | 수신 거부 제거 |

### Settings - 시스템 설정

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/settings/aws` | API Gateway 설정 조회 |
| `PUT` | `/settings/aws` | API Gateway 설정 저장 (ESM DB + SSM 동기화) |
| `POST` | `/settings/aws/test` | API Gateway 연결 테스트 |

### Scheduler - 예약 발송

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/scheduler/job` | 예약 발송 작업 생성 |
| `GET` | `/scheduler/jobs` | 작업 목록 조회 |
| `PUT` | `/scheduler/job/pause` | 작업 일시정지 |
| `PUT` | `/scheduler/job/resume` | 작업 재개 |
| `PUT` | `/scheduler/job/stop` | 작업 중지 |
| `DELETE` | `/scheduler/job` | 작업 삭제 |
| `DELETE` | `/scheduler/job/all` | 전체 작업 삭제 |

## Key Features

### 멀티테넌트 API Key 인증
- 각 테넌트별 API Key 발급 (DB 조회 기반)
- `Authorization` 헤더로 전달, `TenantContext`(ThreadLocal)에 테넌트 정보 설정
- 레거시 단일 API Key(`security.api-key`) 병행 지원

### SES 인증 2가지 방식
- **도메인 인증 (DKIM)**: 온보딩 시 도메인 전체를 SES에 등록, DNS CNAME 레코드 추가로 인증
- **이메일 개별 인증**: DNS 접근 불가 시 이메일 주소 단위로 SES 인증 (인증 메일 수신 방식)

### 발신자 이메일 관리 (TENANT_SENDER 테이블)
- 테넌트별 허용 발신자 이메일 목록 관리
- 도메인 제한: 등록된 발신자 이메일은 테넌트 도메인과 일치해야 함
- 이메일 발송 시 `SenderValidationService`가 발신자 검증

### 발송결과 수신 2-path
- **실시간 Callback**: SES → SNS → Lambda → ESM `/ses/callback/event` 호출 (X-Callback-Secret 검증)
- **보정 폴링**: ESM → API Gateway `GET /results` → DynamoDB 조회 (5~10분 주기, 멱등 처리)

### 설정 UI → SSM 동기화
- `PUT /settings/aws` 저장 시 ESM DB + API Gateway `PUT /config` 경유로 SSM Parameter Store 자동 동기화
- Lambda `event-processor`가 SSM을 30초 캐시로 읽어 모드/콜백 즉시 반영

### AWS CDK 인프라 자동 구축
- `aws/ems-cdk/`에서 `cdk deploy` 한 번으로 전체 AWS 인프라 구축
- 배포 완료 후 출력되는 `ApiGatewayUrl`을 ESM 설정 화면에 입력

## Getting Started

### Prerequisites
- Java 17 (JDK 17)
- PostgreSQL 17+
- Docker
- Node.js 18+ (CDK 배포 시)
- AWS CDK CLI (`npm install -g aws-cdk`)

### 1. AWS 인프라 구축 (CDK)

```bash
cd aws/ems-cdk
npm install
cdk bootstrap   # 최초 1회
cdk deploy
# 출력된 ApiGatewayUrl을 ESM 설정 화면에 입력
```

### 2. PostgreSQL 준비

```bash
cd backend

# Docker로 PostgreSQL 실행
docker-compose up -d postgres

# 테이블 생성 (최초 1회)
docker exec -i ems-postgres psql -U esm -d esm < src/main/resources/sql/V1__init_tables.sql
docker exec -i ems-postgres psql -U esm -d esm < src/main/resources/sql/V2__suppression_table.sql
docker exec -i ems-postgres psql -U esm -d esm < src/main/resources/sql/quartz_tables_postgres.sql
```

### 3. 백엔드 실행

```bash
cd backend

# Windows
start.bat dev

# Linux/Mac
./gradlew bootJar
java -Dspring.profiles.active=dev -jar build/libs/ems.jar
```

### 4. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev     # http://localhost:3000
```

### 주요 URL

| URL | Description |
|-----|-------------|
| http://localhost:7092/swagger-ui.html | Swagger UI |
| http://localhost:7092/v3/api-docs | OpenAPI Docs |
| http://localhost:7092/actuator/health | Health Check |
| http://localhost:3000 | Frontend Admin Dashboard |

## Deployment

### Backend 배포

1. `cd backend && ./gradlew bootJar` → `build/libs/ems.jar` 생성
2. JAR + `start.bat`(또는 start.sh) 서버에 업로드
3. `spring.profiles.active=prod`로 실행
4. 운영 외부 설정: `/svc/ems/config/ems-config-prod.yml` (DB 접속 정보, API Key 등)

### 환경변수

| Variable | Description | Dev Default |
|----------|-------------|-------------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/esm` |
| `DB_USERNAME` | DB 사용자명 | `esm` |
| `DB_PASSWORD` | DB 비밀번호 | `esm` |
| `QUARTZ_DB_URL` | Quartz용 DB URL | `jdbc:postgresql://localhost:5432/esm` |
| `QUARTZ_DB_USER` | Quartz용 사용자명 | `esm` |
| `QUARTZ_DB_PASSWORD` | Quartz용 비밀번호 | `esm` |
| `API_KEY` | 레거시 단일 API Key | (미설정 시 인증 비활성화) |

## Backend Module Structure

```
backend/src/main/java/com/msas/
├── common/
│   ├── exceptionhandler/    # 글로벌 예외 처리 (GlobalControllerAdvice)
│   ├── httplog/             # HTTP 요청/응답 로깅 인터셉터
│   ├── security/            # ApiKeyAuthenticationFilter, CallbackSecretFilter,
│   │                        # TenantContextFilter, SecurityConfig
│   ├── swagger/             # OpenAPI 설정
│   └── tenant/              # TenantContext (ThreadLocal)
├── tenant/                  # 테넌트 CRUD, API Key, 할당량, 발신자 이메일 관리
├── ses/
│   ├── controller/          # EmailController (발송, 템플릿)
│   ├── configset/           # SES ConfigSet 관리
│   ├── configuration/       # AWSSESv2Configuration (미사용 - API Gateway 경유로 대체)
│   └── identity/            # SES 도메인 아이덴티티 관리
├── callback/                # SES 이벤트 콜백 수신 및 상태 업데이트
├── onboarding/              # 테넌트 온보딩 워크플로우 (도메인/이메일 인증)
├── suppression/             # 수신 거부 목록 관리 (BOUNCE/COMPLAINT)
├── settings/                # API Gateway 설정 관리, SSM 동기화
├── scheduler/               # Quartz 예약 발송 관리
└── pollingchecker/          # 보정 폴링 (발송 결과 DynamoDB 조회)
```
