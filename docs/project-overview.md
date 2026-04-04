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
│                    Backend (Spring Boot)                        │
│                        Port: 7092                              │
│                                                                │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌────────────────┐  │
│  │ SES      │ │ Tenant   │ │ Scheduler │ │ Polling        │  │
│  │ Module   │ │ Module   │ │ Module    │ │ Checker        │  │
│  └────┬─────┘ └────┬─────┘ └─────┬─────┘ └───────┬────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌────────────────┐  │
│  │ Callback │ │Onboarding│ │Suppression│ │ SES Identity   │  │
│  │ Module   │ │ Module   │ │ Module    │ │ / ConfigSet    │  │
│  └────┬─────┘ └────┬─────┘ └─────┬─────┘ └───────┬────────┘  │
└───────┼────────────┼─────────────┼───────────────┼────────────┘
        │            │             │               │
   ┌────▼────┐  ┌────▼──────┐ ┌───▼──────┐  ┌─────▼───────┐
   │ AWS SES │  │PostgreSQL │ │PostgreSQL│  │ AWS         │
   │ (v2 SDK)│  │ (Tenant)  │ │ (Quartz) │  │ DynamoDB    │
   └─────────┘  └───────────┘ └──────────┘  └─────────────┘
```

### 모듈 설명

| Module | Description | 외부 연동 |
|--------|-------------|-----------|
| SES Module | 이메일 발송 및 템플릿 관리 | AWS SES v2 |
| Tenant Module | 멀티테넌트 고객사 관리, API Key 발급, 할당량 관리 | PostgreSQL |
| Scheduler Module | Quartz 기반 예약 발송 관리 | PostgreSQL |
| Polling Checker | 발송 대기 이메일 자동 처리 및 결과 추적 | PostgreSQL, AWS DynamoDB |
| Callback Module | SES 이벤트 콜백 수신 및 상태 업데이트 | - |
| Onboarding Module | 테넌트 온보딩 워크플로우 (생성→도메인 인증→활성화) | AWS SES v2 |
| Suppression Module | 수신 거부(Bounce/Complaint) 목록 관리 | PostgreSQL |
| SES Identity | SES 도메인 아이덴티티 등록 및 DKIM 관리 | AWS SES v2 |
| SES ConfigSet | 테넌트별 SES 구성 세트 관리 | AWS SES v2 |

### 기술 스택

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.1, Java 17 |
| Frontend | React 19, Vite 7, TypeScript 5.9 |
| Database | PostgreSQL 17, AWS DynamoDB |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz (DB Store) |
| Cloud | AWS SES SDK v2, AWS DynamoDB SDK v2 |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Security | Spring Security (API Key) |
| Build | Gradle, Docker (Multi-stage) |
| Dev Tools | LocalStack (AWS Mock) |
| Monitoring | Spring Actuator |

### 데이터 흐름

#### 이메일 발송 흐름

```
1. [외부 시스템] → PostgreSQL에 이메일 데이터 INSERT (상태: SR)
2. [Polling Checker] → 60초 주기로 SR 상태 이메일 조회 (최대 280건)
3. [Polling Checker] → Quartz 스케줄러에 발송 작업 등록 (상태: SS)
4. [Scheduler] → AWS SES API로 이메일 발송 (상태: SE)
5. [AWS SES] → SNS → Lambda → DynamoDB에 이벤트 기록
6. [Polling Checker] → 60초 주기로 DynamoDB 이벤트 조회 (최대 300건)
7. [Polling Checker] → PostgreSQL에 최종 상태 업데이트 (SD/SB/SC)
```

#### 직접 API 발송 흐름

```
1. [Client] → POST /ses/text-mail 또는 /ses/templated-mail
2. [Backend] → AWS SES API 직접 호출
3. [Backend] → messageId 반환
```

### 배포 환경

| Environment | Profile | Description |
|-------------|---------|-------------|
| Local | `local` | 로컬 개발 (Docker Compose) |
| Development | `dev` | 개발 서버 |
| Production | `prod` | 운영 서버 (외부 설정 파일 참조) |
