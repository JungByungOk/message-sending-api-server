# Message Sending API Server (NTE)

멀티채널 메시지 발송 API 서버 - AWS SES 이메일, Telegram, Slack 메시지를 통합 관리하는 Spring Boot 기반 알림 서버

## Project Structure

```
message-sending-api-server/
├── backend/                # Spring Boot API 서버
│   ├── src/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── ...
├── frontend/               # (예정) 관리 대시보드
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

## Tech Stack

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.4.1 |
| Language | Java 17 |
| Database | MariaDB 11, AWS DynamoDB |
| ORM | MyBatis 3.0.4 |
| Scheduler | Quartz (DB 기반) |
| Cloud | AWS SES (SDK v2), AWS DynamoDB (SDK v2) |
| Messaging | Telegram Bot API 7.11.0 |
| Security | Spring Security (API Key 인증) |
| Build | Gradle, Docker (Multi-stage) |
| Etc | Guava (RateLimiter), Gson, Lombok |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    API Server (NTE)                      │
│                     Port: 7092                           │
├──────────┬──────────────┬────────────┬──────────────────┤
│  SES     │  Telegram    │  Scheduler │  Polling Checker │
│  Module  │  Module      │  Module    │  Module          │
├──────────┴──────────────┴────────────┴──────────────────┤
│              Spring Security (API Key)                   │
├─────────────────────────────────────────────────────────┤
│  MariaDB (이메일/스케줄러)  │  DynamoDB (SES 이벤트)     │
└─────────────────────────────────────────────────────────┘
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

### Telegram - 메시지 발송

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/telegram/message` | 채널별 메시지 발송 |
| `GET` | `/telegram/info` | 봇 정보 조회 |
| `GET` | `/telegram/ids` | 등록된 채널 ID 조회 |

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

### Quartz 스케줄러
- DB 기반 예약 발송 (MariaDB)
- 작업 상태 관리 (RUNNING, SCHEDULED, PAUSED, COMPLETE)
- 작업 일시정지/재개/중지
- Thread Pool: 10 threads

### Polling Checker
- **신규 이메일 폴링**: MariaDB에서 60초 주기로 대기 이메일 확인 (최대 280건/회)
- **발송 결과 폴링**: DynamoDB에서 60초 주기로 SES 이벤트 확인 (최대 300건/회)
- Delivery, Bounce, Complaint 이벤트 추적
- Blacklist 이메일 주소 필터링

### Telegram
- 멀티채널 메시지 발송
- 채널명 기반 발송 (채널 ID 자동 매핑)
- Long Polling 방식 업데이트 수신

### 보안
- API Key 기반 인증
- Health Check 엔드포인트 공개 (`/actuator/health`, `/actuator/info`)
- HTTP 요청/응답 로깅

## Getting Started

### Prerequisites
- Java 17+
- MariaDB 11+
- AWS 계정 (SES, DynamoDB)
- Telegram Bot Token (선택)

### Docker 실행

```bash
cd backend

# 환경변수 설정
export AWS_ACCESS_KEY=your-access-key
export AWS_SECRET_KEY=your-secret-key
export TELEGRAM_BOT_TOKEN=your-bot-token
export TELEGRAM_CHAT_ID=your-chat-id
export API_KEY=your-api-key

# Docker Compose 실행
docker-compose up -d
```

### 로컬 빌드

```bash
cd backend

# 빌드
./gradlew bootJar

# 실행
java -Dspring.profiles.active=dev -jar build/libs/nte.jar
```

## Deployment

1. `backend/` 디렉토리에서 `gradle bootJar` 빌드 → `build/libs/nte.jar` 생성
2. `backend/deploy/bin` 폴더의 파일을 서버 `/svc/nte` 경로에 업로드
3. `/svc/nte/script/start.sh` 스크립트로 서버 구동

### 주의사항

- **프로필 설정**: `start.sh` 내 `-Dspring.profiles.active={dev|prod}` 확인
- **외부 설정**: `/svc/nte/config/nte-config.yml` 파일로 내부 Properties 오버라이드 가능
  - Database 접속 정보
  - AWS 인증 정보
  - Telegram 봇 설정

## Backend Structure

```
backend/src/main/java/com/msas/
├── common/
│   ├── exceptionhandler/    # 글로벌 예외 처리
│   ├── httplog/             # HTTP 요청/응답 로깅
│   ├── security/            # API Key 인증
│   └── utils/               # 유틸리티
├── ses/                     # AWS SES 이메일 모듈
├── telegram/                # Telegram 봇 모듈
├── scheduler/               # Quartz 스케줄러 모듈
└── pollingchecker/          # 이메일 상태 폴링 모듈
```

## Reference

- [AWS SES SDK Spring Boot](https://github.com/Rajithkonara/aws-ses-sdk-spring-boot)
- [Quartz Scheduler Documentation](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Java Telegram Bot API](https://github.com/pengrad/java-telegram-bot-api)