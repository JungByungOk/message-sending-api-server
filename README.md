# Joins ESM

멀티테넌트 이메일 발송 관리 플랫폼 - AWS SES 기반 이메일 발송, 테넌트 온보딩, 수신 거부 관리를 제공하는 Spring Boot + React 풀스택 SaaS 서버

## Project Structure

```
message-sending-api-server/
├── backend/                # Spring Boot API 서버
│   ├── src/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── ...
├── frontend/               # React 19 어드민 대시보드 (Vite + TypeScript)
├── docs/                   # 프로젝트 문서
│   ├── backend-spec.md     # Backend API 명세서
│   └── backend-development-plan.md  # 멀티테넌트 전환 개발 계획
└── README.md
```

## Documentation

| Document | Description |
|----------|-------------|
| [Project Overview](docs/project-overview.md) | 프로젝트 개요, 시스템 구성, 데이터 흐름 |
| [Common Feature Spec](docs/common-feature-spec.md) | 공통 기능 명세 (인증, 에러 처리, 상태 코드 등) |
| [Backend API Spec](docs/backend-spec.md) | Backend REST API 상세 명세서 |
| [Backend Development Plan](docs/backend-development-plan.md) | 멀티테넌트 SaaS 전환 개발 계획 |
| [Frontend Spec](docs/frontend-spec.md) | Frontend 기술 명세 (타입, 상태 관리, 라우팅) |
| [Frontend Development Plan](docs/frontend-development-plan.md) | Frontend 개발 계획 (Phase 1-5) |

## Tech Stack

### Backend

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.4.1 |
| Language | Java 17 |
| Database | PostgreSQL 17, AWS DynamoDB |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz (DB 기반) |
| Cloud | AWS SES (SDK v2), AWS DynamoDB (SDK v2) |
| Security | Spring Security (API Key 인증) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Gradle, Docker (Multi-stage) |
| Dev Tools | LocalStack (AWS Mock) |
| Etc | Guava (RateLimiter), Gson, Lombok |

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

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│           Frontend (React 19 + Vite + TypeScript)        │
└────────────────────────┬────────────────────────────────┘
                         │ REST API
┌────────────────────────▼────────────────────────────────┐
│                  ESM (Spring Boot 7092)                   │
├──────────┬────────────┬────────────┬───────────────────┤
│  SES     │  Tenant    │  Callback  │  Settings         │
│  Module  │  Module    │  Module    │  Module           │
├──────────┼────────────┼────────────┼───────────────────┤
│Onboarding│Suppression │  Scheduler │  SES Identity/    │
│  Module  │  Module    │  Module    │  ConfigSet        │
├──────────┴────────────┴────────────┴───────────────────┤
│    Spring Security (Tenant API Key + Callback Secret)    │
└────────────────────┬───────────────┬────────────────────┘
                     │               │
            ┌────────▼────────┐   ┌──▼──────────┐
            │   PostgreSQL    │   │ API Gateway  │
            └─────────────────┘   └──┬───────────┘
                                     │
                ┌────────────────────┼────────────────────┐
                │                    │                    │
         ┌──────▼──────┐  ┌─────────▼─────────┐  ┌──────▼──────┐
         │    SQS      │  │  SSM Parameter    │  │  Lambda     │
         │  (발송 큐)  │  │  Store (설정)     │  │ event-query │
         └──────┬──────┘  └───────────────────┘  └──────┬──────┘
                │                                       │
         ┌──────▼──────┐                         ┌──────▼──────┐
         │   Lambda    │                         │  DynamoDB   │
         │email-sender │                         │ send-results│
         └──────┬──────┘                         └─────────────┘
                │                                       ▲
         ┌──────▼──────┐    ┌──────────────┐           │
         │  Amazon SES │───▶│ SNS → Lambda │───────────┘
         │             │    │event-processor│
         └─────────────┘    └──────┬───────┘
                                   │ callback 모드
                            ┌──────▼───────┐
                            │ESM /callback │
                            └──────────────┘
```

## API Endpoints

### AWS SES - 이메일 발송

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
| `GET` | `/tenant/list` | 테넌트 목록 조회 |
| `PATCH` | `/tenant/{tenantId}` | 테넌트 수정 |
| `DELETE` | `/tenant/{tenantId}` | 테넌트 비활성화 |
| `DELETE` | `/tenant/{tenantId}/permanent` | 테넌트 영구 삭제 |
| `POST` | `/tenant/{tenantId}/activate` | 테넌트 활성화 |
| `POST` | `/tenant/{tenantId}/regenerate-key` | API Key 재발급 |
| `GET` | `/tenant/{tenantId}/quota` | 할당량 사용 현황 |
| `PATCH` | `/tenant/{tenantId}/quota` | 할당량 수정 |

### Onboarding - 테넌트 온보딩

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/onboarding/start` | 온보딩 시작 |
| `GET` | `/onboarding/{tenantId}/status` | 온보딩 상태 조회 |
| `GET` | `/onboarding/{tenantId}/dkim` | DKIM 레코드 조회 |
| `POST` | `/onboarding/{tenantId}/activate` | 테넌트 수동 활성화 |

### SES Callback - 이벤트 콜백

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/callback/event` | SES 이벤트 처리 |
| `GET` | `/ses/callback/health` | 콜백 상태 확인 |

### SES Identity - 도메인 아이덴티티

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/identity` | 도메인 아이덴티티 생성 |
| `GET` | `/ses/identity/{domain}` | 도메인 인증 상태 조회 |
| `DELETE` | `/ses/identity/{domain}` | 도메인 아이덴티티 삭제 |

### SES ConfigSet - 구성 세트

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
| `PUT` | `/settings/aws` | API Gateway 설정 저장 |
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

## Features

### AWS SES 이메일
- HTML 이메일 및 템플릿 기반 이메일 발송
- 템플릿 CRUD 관리
- CC/BCC 수신자 지원
- 메시지 태그 (캠페인/이벤트) 추적
- 첨부파일 지원

### Tenant 관리 (멀티테넌트)
- 고객사별 테넌트 CRUD 관리
- 테넌트별 API Key 발급/재발급
- 도메인 등록 및 인증 상태 관리
- 일/월 발송 쿼터 설정

### Quartz 스케줄러
- DB 기반 예약 발송 (PostgreSQL)
- 작업 상태 관리 (RUNNING, SCHEDULED, PAUSED, COMPLETE)
- 작업 일시정지/재개/중지
- Thread Pool: 10 threads

### Polling Checker
- **신규 이메일 폴링**: PostgreSQL에서 60초 주기로 대기 이메일 확인 (최대 280건/회)
- **발송 결과 폴링**: DynamoDB에서 60초 주기로 SES 이벤트 확인 (최대 300건/회)
- Delivery, Bounce, Complaint 이벤트 추적
- Blacklist 이메일 주소 필터링

### 보안
- API Key 기반 인증
- Health Check 엔드포인트 공개 (`/actuator/health`, `/actuator/info`)
- HTTP 요청/응답 로깅

## Getting Started

### Prerequisites
- Java 17 (빌드/실행용, JDK 25 환경에서는 JDK 17 지정 필요)
- PostgreSQL 17+
- Docker (LocalStack, PostgreSQL)

### Docker 실행

```bash
cd backend

# PostgreSQL + LocalStack 실행
docker-compose up -d postgres localstack

# DynamoDB 테이블 생성 (최초 1회)
docker exec -i nte-localstack awslocal dynamodb create-table \
  --table-name SESEvents \
  --attribute-definitions AttributeName=SESMessageId,AttributeType=S AttributeName=SnsPublishTime,AttributeType=S \
  --key-schema AttributeName=SESMessageId,KeyType=HASH AttributeName=SnsPublishTime,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST --region ap-northeast-2

# PostgreSQL 테이블 생성 (최초 1회)
docker exec -i nte-postgres psql -U nft -d nft < src/main/resources/sql/V1__init_tables.sql
docker exec -i nte-postgres psql -U nft -d nft < src/main/resources/sql/quartz_tables_postgres.sql
```

### 로컬 빌드 (Windows)

```bash
cd backend
start.bat dev      # 빌드 + 실행 (dev 프로필)
start.bat prod     # 빌드 + 실행 (prod 프로필)
```

### 로컬 빌드 (Linux/Mac)

```bash
cd backend
./gradlew bootJar
java -Dspring.profiles.active=dev -jar build/libs/nte.jar
```

### 주요 URL

| URL | Description |
|-----|-------------|
| http://localhost:7092/swagger-ui/index.html | Swagger UI |
| http://localhost:7092/v3/api-docs | OpenAPI Docs |
| http://localhost:7092/actuator/health | Health Check |

## Deployment

1. `backend/` 디렉토리에서 `gradle bootJar` 빌드 → `build/libs/nte.jar` 생성
2. `backend/deploy/bin` 폴더의 파일을 서버 `/svc/nte` 경로에 업로드
3. `/svc/nte/script/start.sh` 스크립트로 서버 구동

### 주의사항

- **프로필 설정**: `start.sh` 내 `-Dspring.profiles.active={dev|prod}` 확인
- **외부 설정**: `/svc/nte/config/nte-config.yml` 파일로 내부 Properties 오버라이드 가능
  - Database 접속 정보
  - AWS 인증 정보

## Backend Structure

```
backend/src/main/java/com/msas/
├── common/
│   ├── exceptionhandler/    # 글로벌 예외 처리
│   ├── httplog/             # HTTP 요청/응답 로깅
│   ├── security/            # API Key 인증, TenantContextFilter
│   ├── swagger/             # Swagger 설정
│   ├── tenant/              # TenantContext (ThreadLocal)
│   └── utils/               # 유틸리티
├── tenant/                  # 테넌트 관리 모듈 (할당량 포함)
├── ses/                     # AWS SES 이메일 모듈
│   ├── configset/           # SES ConfigSet 관리
│   ├── configuration/       # SES v2 클라이언트 설정
│   └── identity/            # SES 도메인 아이덴티티 관리
├── callback/                # SES 이벤트 콜백 모듈
├── onboarding/              # 테넌트 온보딩 워크플로우
├── suppression/             # 수신 거부 목록 관리
├── scheduler/               # Quartz 스케줄러 모듈
└── pollingchecker/          # 이메일 상태 폴링 모듈
```

## Reference

- [AWS SES SDK Spring Boot](https://github.com/Rajithkonara/aws-ses-sdk-spring-boot)
- [Quartz Scheduler Documentation](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [LocalStack](https://docs.localstack.cloud/)