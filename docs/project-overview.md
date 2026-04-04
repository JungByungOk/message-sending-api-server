# Project Overview

## ESM (Email Sending Management)

멀티테넌트 이메일 발송 관리 플랫폼

### 목적

다양한 채널(이메일, Slack)을 통한 메시지 발송을 하나의 API 서버에서 통합 관리하며, 멀티테넌트 SaaS 아키텍처를 지원합니다.

### 시스템 구성

```
┌───────────────────────────────────────────────────────────────┐
│              Client / Frontend (React 19 + Vite)               │
└──────────────────────────┬────────────────────────────────────┘
                           │ REST API (Tenant API Key Auth)
┌──────────────────────────▼────────────────────────────────────┐
│                  ESM Backend (Spring Boot)                      │
│                        Port: 7092                              │
│                                                                │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌────────────────┐  │
│  │ SES      │ │ Tenant   │ │ Callback  │ │ Settings       │  │
│  │ Module   │ │ Module   │ │ Module    │ │ Module         │  │
│  └────┬─────┘ └────┬─────┘ └───────────┘ └────────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌────────────────┐  │
│  │Onboarding│ │Suppression│ │ Scheduler │ │ SES Identity  │  │
│  │ Module   │ │ Module   │ │ Module    │ │ / ConfigSet    │  │
│  └──────────┘ └──────────┘ └─────┬─────┘ └────────────────┘  │
└───────┼────────────┼─────────────┼────────────────────────────┘
        │            │             │
   ┌────▼──────────────────┐  ┌───▼──────┐
   │  AWS API Gateway      │  │PostgreSQL│
   │  (IP Whitelist, IAM)  │  │          │
   └────────┬──────────────┘  └──────────┘
            │
   ┌────────▼──────────────┐
   │  SQS (공통 Queue)     │
   └────────┬──────────────┘
            │
   ┌────────▼──────────────┐
   │  Lambda (email sender)│
   └────────┬──────────────┘
            │
   ┌────────▼──────────────┐
   │  Amazon SES           │
   └───────────────────────┘
```

### 모듈 설명

| Module | Description | 외부 연동 |
|--------|-------------|-----------|
| SES Module | 이메일 발송 요청 (API Gateway 경유) | AWS API Gateway |
| Tenant Module | 멀티테넌트 고객사 관리, API Key 발급, 할당량 관리 | PostgreSQL |
| Scheduler Module | Quartz 기반 예약 발송 관리 | PostgreSQL |
| Callback Module | SES 이벤트 콜백 수신 및 상태 업데이트 | - |
| Onboarding Module | 테넌트 온보딩 워크플로우 (생성→도메인 인증→활성화) | API Gateway |
| Suppression Module | 수신 거부(Bounce/Complaint) 목록 관리 | PostgreSQL |
| SES Identity | SES 도메인 아이덴티티 등록 및 DKIM 관리 | API Gateway |
| SES ConfigSet | 테넌트별 SES 구성 세트 관리 | API Gateway |
| Settings Module | API Gateway 연결, Callback, 수신 모드 설정 관리 | PostgreSQL, SSM Parameter Store |

### 기술 스택

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.1, Java 17 |
| Frontend | React 19.2, Vite 8, TypeScript 5.9, Ant Design 5.29 |
| Database | PostgreSQL 17, AWS DynamoDB |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz (DB Store) |
| Cloud | AWS API Gateway (SQS → Lambda → SES) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Security | Spring Security (API Key) |
| Build | Gradle, Docker (Multi-stage) |
| Dev Tools | LocalStack (AWS Mock) |
| Monitoring | Spring Actuator |

### 데이터 흐름

#### 이메일 발송 흐름

```
1. [ESM] → API Gateway POST /send-email (발송 요청)
2. [API Gateway] → SQS (비동기 큐잉)
3. [Lambda email-sender] → SQS 트리거 → SES 이메일 발송
4. [Lambda email-sender] → DynamoDB ems-idempotency (중복 발송 방지)
5. [Lambda email-sender] → DynamoDB ems-tenant-config (테넌트 Config Set 조회)
```

#### 발송 결과 수신 흐름 (2-path)

```
[실시간 - Callback 모드]
1. [SES] → SNS → Lambda event-processor
2. [Lambda] → DynamoDB ems-send-results 저장 (항상)
3. [Lambda] → SSM에서 모드 확인 → ESM /ses/callback/event 호출
4. [ESM] → X-Callback-Secret 검증 → PostgreSQL 상태 업데이트

[보정 - 항상 동작]
1. [ESM] → API Gateway GET /results?tenant_id=X&after=T (5~10분 주기)
2. [Lambda event-query] → DynamoDB ems-send-results GSI Query
3. [ESM] → 멱등성 처리로 PostgreSQL 상태 업데이트
```

#### 설정 동기화 흐름

```
1. [ESM 설정 UI] → PUT /settings/aws (ESM DB 저장)
2. [ESM] → API Gateway PUT /config (SSM Parameter Store 동기화)
3. [Lambda event-processor] → SSM 읽기 (캐시 30초) → 모드/콜백 자동 반영
```

### 배포 환경

| Environment | Profile | Description |
|-------------|---------|-------------|
| Local | `local` | 로컬 개발 (Docker Compose) |
| Development | `dev` | 개발 서버 |
| Production | `prod` | 운영 서버 (외부 설정 파일 참조) |
