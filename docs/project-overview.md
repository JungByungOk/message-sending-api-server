# Project Overview

## NTE (Notification & Template Engine)

멀티채널 메시지 발송 통합 플랫폼

### 목적

다양한 채널(이메일, Telegram, Slack)을 통한 메시지 발송을 하나의 API 서버에서 통합 관리합니다.

### 시스템 구성

```
┌───────────────────────────────────────────────────────────────┐
│                        Client / Frontend                       │
└──────────────────────────┬────────────────────────────────────┘
                           │ REST API (API Key Auth)
┌──────────────────────────▼────────────────────────────────────┐
│                    Backend (Spring Boot)                        │
│                        Port: 7092                              │
│                                                                │
│  ┌──────────┐ ┌───────────┐ ┌───────────┐ ┌────────────────┐ │
│  │ SES      │ │ Telegram  │ │ Scheduler │ │ Polling        │ │
│  │ Module   │ │ Module    │ │ Module    │ │ Checker        │ │
│  └────┬─────┘ └─────┬─────┘ └─────┬─────┘ └───────┬────────┘ │
└───────┼─────────────┼─────────────┼───────────────┼───────────┘
        │             │             │               │
   ┌────▼────┐  ┌─────▼─────┐ ┌────▼─────┐  ┌──────▼──────┐
   │ AWS SES │  │ Telegram  │ │ MariaDB  │  │ AWS         │
   │         │  │ Bot API   │ │ (Quartz) │  │ DynamoDB    │
   └─────────┘  └───────────┘ └──────────┘  └─────────────┘
```

### 모듈 설명

| Module | Description | 외부 연동 |
|--------|-------------|-----------|
| SES Module | 이메일 발송 및 템플릿 관리 | AWS SES |
| Telegram Module | 멀티채널 Telegram 메시지 발송 | Telegram Bot API |
| Scheduler Module | Quartz 기반 예약 발송 관리 | MariaDB |
| Polling Checker | 발송 대기 이메일 자동 처리 및 결과 추적 | MariaDB, AWS DynamoDB |

### 기술 스택

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.1, Java 17 |
| Frontend | (예정) |
| Database | MariaDB 11, AWS DynamoDB |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz (DB Store) |
| Cloud | AWS SES SDK v2, AWS DynamoDB SDK v2 |
| Messaging | Telegram Bot API 7.11.0 |
| Security | Spring Security (API Key) |
| Build | Gradle, Docker (Multi-stage) |
| Monitoring | Spring Actuator |

### 데이터 흐름

#### 이메일 발송 흐름

```
1. [외부 시스템] → MariaDB에 이메일 데이터 INSERT (상태: SR)
2. [Polling Checker] → 60초 주기로 SR 상태 이메일 조회 (최대 280건)
3. [Polling Checker] → Quartz 스케줄러에 발송 작업 등록 (상태: SS)
4. [Scheduler] → AWS SES API로 이메일 발송 (상태: SE)
5. [AWS SES] → SNS → Lambda → DynamoDB에 이벤트 기록
6. [Polling Checker] → 60초 주기로 DynamoDB 이벤트 조회 (최대 300건)
7. [Polling Checker] → MariaDB에 최종 상태 업데이트 (SD/SB/SC)
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
