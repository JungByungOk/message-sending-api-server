# Backend API Specification

## Overview

**Application Name**: NTE (Notification & Template Engine)
**Base URL**: `http://{host}:7092`
**Authentication**: API Key (Header 기반)
**Framework**: Spring Boot 3.4.1 / Java 17

---

## Authentication

모든 API 요청은 API Key 인증이 필요합니다. (Health Check 제외)

```
Header: Authorization: {API_KEY}
```

### Public Endpoints (인증 불필요)
- `GET /actuator/health`
- `GET /actuator/info`
- `POST /ses/feedback/**` (AWS SNS Callback)

---

## 1. AWS SES - 이메일 발송

### 1.1 텍스트 이메일 발송

```
POST /ses/text-mail
```

**Request Body**
```json
{
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "subject": "이메일 제목",
  "body": "<h1>HTML 본문</h1>",
  "tags": [
    {
      "name": "campaign",
      "value": "welcome-email"
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| from | String (Email) | Y | 발신자 이메일 |
| to | String (Email) | Y | 수신자 이메일 |
| subject | String | Y | 이메일 제목 |
| body | String | Y | HTML 본문 |
| tags | List\<MessageTag\> | Y | 추적용 태그 (캠페인/이벤트명) |

**Response** `200 OK`
```json
{
  "messageId": "aws-ses-message-id"
}
```

---

### 1.2 템플릿 이메일 발송

```
POST /ses/templated-mail
```

**Request Body**
```json
{
  "templateName": "Welcome-Template",
  "from": "sender@example.com",
  "to": ["recipient1@example.com", "recipient2@example.com"],
  "cc": ["cc@example.com"],
  "bcc": ["bcc@example.com"],
  "templateData": {
    "user_name": "홍길동",
    "company": "MyCompany"
  },
  "tags": [
    {
      "name": "campaign",
      "value": "welcome"
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| templateName | String | Y | SES 템플릿 이름 |
| from | String | Y | 발신자 이메일 |
| to | List\<String\> | Y | 수신자 목록 |
| cc | List\<String\> | N | 참조 목록 |
| bcc | List\<String\> | N | 숨은참조 목록 |
| templateData | Map\<String, String\> | Y | 템플릿 변수 데이터 |
| tags | List\<MessageTag\> | Y | 추적용 태그 |

**Response** `200 OK`
```json
{
  "messageId": "aws-ses-message-id"
}
```

---

### 1.3 템플릿 생성

```
POST /ses/template
```

**Request Body**
```json
{
  "templateName": "Welcome-Template",
  "subjectPart": "안녕하세요, {{user_name}}님",
  "htmlPart": "<h1>Welcome {{user_name}}</h1>",
  "textPart": "Welcome {{user_name}}"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| templateName | String | Y | 템플릿 이름 |
| subjectPart | String | Y | 제목 템플릿 |
| htmlPart | String | Y | HTML 본문 템플릿 |
| textPart | String | Y | 텍스트 본문 템플릿 |

**Response** `200 OK`
```json
{
  "awsRequestId": "aws-request-id"
}
```

---

### 1.4 템플릿 수정

```
PATCH /ses/template
```

**Request/Response**: 1.3 템플릿 생성과 동일

---

### 1.5 템플릿 삭제

```
DELETE /ses/template
```

**Request Body**
```json
{
  "templateName": "Welcome-Template"
}
```

**Response** `200 OK`
```json
{
  "awsRequestId": "aws-request-id"
}
```

---

### 1.6 템플릿 목록 조회

```
GET /ses/templates
```

**Response** `200 OK`
```json
[
  {
    "name": "Welcome-Template",
    "createdTimestamp": "2024-01-01T00:00:00Z"
  }
]
```

---

## 3. Scheduler - 예약 발송

### 3.1 예약 작업 생성

```
POST /scheduler/job
```

**Request Body**
```json
{
  "jobName": "welcome-email-job",
  "jobGroup": "DEFAULT",
  "description": "신규 가입자 환영 이메일 발송",
  "startDateAt": "2024-12-31T14:30:00",
  "templateName": "Welcome-Template",
  "from": "no-reply@example.com",
  "templatedEmailList": [
    {
      "id": "email-001",
      "to": ["user1@example.com"],
      "cc": [],
      "bcc": [],
      "templateParameters": {
        "user_name": "홍길동"
      }
    },
    {
      "id": "email-002",
      "to": ["user2@example.com"],
      "templateParameters": {
        "user_name": "김영희"
      }
    }
  ],
  "tags": [
    {
      "name": "campaign",
      "value": "welcome-2024"
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| jobName | String | Y | 작업 이름 (고유값) |
| jobGroup | String | N | 작업 그룹 (기본: DEFAULT) |
| description | String | N | 작업 설명 |
| startDateAt | LocalDateTime | N | 예약 시간 (미래 시간만 허용, 미지정 시 즉시 실행) |
| templateName | String | Y | SES 템플릿 이름 |
| from | String | Y | 발신자 이메일 |
| templatedEmailList | List\<TemplatedEmail\> | Y | 발송 대상 목록 |
| tags | List\<MessageTag\> | Y | 추적용 태그 |

**TemplatedEmail**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | String | N | 이메일 식별자 (SES messageId 매핑용) |
| to | List\<String\> | Y | 수신자 목록 |
| cc | List\<String\> | N | 참조 목록 |
| bcc | List\<String\> | N | 숨은참조 목록 |
| templateParameters | Map\<String, String\> | Y | 템플릿 변수 |

**Response** `201 Created`
```json
{
  "result": true,
  "message": "Job created successfully"
}
```

**Error** `400 Bad Request`
```json
{
  "result": false,
  "message": "Job already exits"
}
```

---

### 3.2 작업 목록 조회

```
GET /scheduler/jobs
```

**Response** `200 OK`
```json
{
  "numOfAllJobs": 3,
  "numOfRunningJobs": 1,
  "numOfGroups": 1,
  "jobs": [
    {
      "jobName": "welcome-email-job",
      "groupName": "DEFAULT",
      "scheduleTime": "2024-12-31T14:30:00",
      "lastFiredTime": "2024-12-31T14:30:00",
      "nextFireTime": null,
      "jobStatus": "COMPLETE"
    }
  ]
}
```

**Job Status**: `RUNNING`, `SCHEDULED`, `PAUSED`, `COMPLETE`

---

### 3.3 작업 일시정지

```
PUT /scheduler/job/pause
```

**Request Body**
```json
{
  "jobName": "welcome-email-job",
  "jobGroup": "DEFAULT"
}
```

**Response** `200 OK`
```json
{
  "result": true,
  "message": "Job paused successfully"
}
```

---

### 3.4 작업 재개

```
PUT /scheduler/job/resume
```

**Request/Response**: 3.3과 동일한 Request Body 형식

**Response** `200 OK`
```json
{
  "result": true,
  "message": "Job resumed successfully"
}
```

---

### 3.5 작업 중지

```
PUT /scheduler/job/stop
```

**Request/Response**: 3.3과 동일한 Request Body 형식

**Response** `200 OK`
```json
{
  "result": true,
  "message": "Job stop successfully"
}
```

---

### 3.6 작업 삭제

```
DELETE /scheduler/job
```

**Request/Response**: 3.3과 동일한 Request Body 형식

**Response** `200 OK`
```json
{
  "result": true,
  "message": "Job deleted successfully"
}
```

---

### 3.7 전체 작업 삭제

```
DELETE /scheduler/job/all
```

**Response** `200 OK`
```json
{
  "result": true,
  "message": "All job deleted successfully - [jobKey1, jobKey2]"
}
```

---

## 4. Polling Checker (내부 스케줄링)

외부 API 없이 내부적으로 동작하는 자동화 프로세스입니다.

### 4.1 신규 이메일 폴링

- **주기**: 60초
- **동작**: PostgreSQL에서 발송 대기 상태(`SR`)인 이메일을 최대 280건 조회 후 Quartz 스케줄러에 자동 등록
- **조회 범위**: 현재 시점으로부터 1주일 이내 데이터

### 4.2 발송 결과 폴링

- **주기**: 60초
- **동작**: AWS DynamoDB에서 SES 이벤트를 최대 300건 조회 후 PostgreSQL에 최종 상태 업데이트
- **이벤트 유형**: Delivery (발송 완료), Bounce (반송), Complaint (수신 거부)

---

## 5. Database Schema

### 5.1 주요 테이블

| Table | Description |
|-------|-------------|
| `ADM_EMAIL_SEND_MST` | 이메일 발송 마스터 (캠페인 단위) |
| `ADM_EMAIL_SEND_DTL` | 이메일 발송 상세 (수신자 단위) |
| `ADM_EMAIL_ATTCH_FILE_LST` | 첨부파일 목록 |

### 5.2 이메일 상태 코드

| Code | Description |
|------|-------------|
| `SR` | 발송 대기 (Send Ready) |
| `SS` | 스케줄러 등록 완료 |
| `SE` | AWS SES 발송 완료 |
| `SD` | 최종 발송 완료 (Delivery) |
| `SB` | 반송 (Bounce) |
| `SC` | 수신 거부 (Complaint) |
| `SF` | 발송 실패 (Fail) |

### 5.3 발송 구분 코드 (EnumEmailSendDivisionCode)

| Code | Description |
|------|-------------|
| `T` | 텍스트 이메일 |
| `H` | HTML 이메일 |
| `P` | 템플릿 이메일 |

---

## 6. Configuration

### 6.1 Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_URL` | PostgreSQL JDBC URL | Y |
| `DB_USERNAME` | PostgreSQL 사용자명 | Y |
| `DB_PASSWORD` | PostgreSQL 비밀번호 | Y |
| `QUARTZ_DB_URL` | Quartz용 PostgreSQL URL | Y |
| `QUARTZ_DB_USER` | Quartz용 사용자명 | Y |
| `QUARTZ_DB_PASSWORD` | Quartz용 비밀번호 | Y |
| `AWS_ACCESS_KEY` | AWS Access Key | Y (dev: `test`) |
| `AWS_SECRET_KEY` | AWS Secret Key | Y (dev: `test`) |
| `AWS_ENDPOINT` | AWS Endpoint Override (LocalStack) | N (dev: `http://localhost:4566`) |
| `API_KEY` | API 인증 키 | N (미설정 시 인증 비활성화) |

### 6.2 Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `7092` | 서버 포트 |
| `spring.lifecycle.timeout-per-shutdown-phase` | `30s` | Graceful shutdown 대기 시간 |
| `aws.ses.region` | `ap-northeast-2` | AWS SES 리전 |
| `aws.dynamo.region` | `ap-northeast-2` | AWS DynamoDB 리전 |
| `polling.schedule.send-email-check-time` | `60000` | 신규 이메일 폴링 주기 (ms) |
| `polling.schedule.send-email-event-check-time` | `60000` | 발송 결과 폴링 주기 (ms) |
| `spring.quartz.threadPool.threadCount` | `10` | Quartz 스레드 풀 크기 |

### 6.3 Profiles

| Profile | Description |
|---------|-------------|
| `local` | 로컬 개발 환경 |
| `dev` | 개발 서버 환경 |
| `prod` | 운영 서버 환경 (외부 설정 파일 참조) |

---

## 7. Error Response

모든 에러는 공통 형식으로 반환됩니다.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "에러 상세 메시지"
}
```

| HTTP Status | Description |
|-------------|-------------|
| `400` | 잘못된 요청 (Validation 실패, 중복 작업 등) |
| `401` | 인증 실패 (API Key 누락/불일치) |
| `500` | 서버 내부 오류 (AWS SES 연동 실패 등) |
