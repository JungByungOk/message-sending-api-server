# Backend API Specification

## Overview

**Application Name**: ESM (Email Sending Management)
**Base URL**: `http://{host}:7092`
**Authentication**: Tenant API Key (Header 기반)
**Framework**: Spring Boot 3.4.1 / Java 17

> 모든 AWS 연동(SES 발송, Identity, ConfigSet, 템플릿)은 AWS SDK 없이 `ApiGatewayClient`(Java HTTP Client)가 API Gateway를 직접 호출합니다.

---

## Authentication

### JWT 인증 (Admin Dashboard)

Admin Dashboard는 JWT Bearer 토큰으로 인증합니다.

```
Header: Authorization: Bearer {JWT_ACCESS_TOKEN}
```

- **필터 체인**: `JwtAuthenticationFilter` → `ApiKeyAuthenticationFilter` → `TenantContextFilter`
- **JWT 판별**: 토큰에 `.`이 2개 포함되면 JWT로 처리

### Tenant API Key 인증

테넌트 API 요청은 API Key 인증이 필요합니다. (Public Endpoints 제외)

```
Header: Authorization: {TENANT_API_KEY}
또는
Header: Authorization: Bearer {TENANT_API_KEY}
```

- **Tenant API Key**: 테넌트 생성 시 발급되며 DB에서 조회하여 인증
- **레거시 단일 Key**: 환경변수 `API_KEY` 설정 시 병행 지원
- **미설정 시**: `API_KEY` 환경변수가 없으면 인증 비활성화 (개발용)

### Public Endpoints (인증 불필요)

| Endpoint | Description |
|----------|-------------|
| `POST /auth/login` | 로그인 |
| `POST /auth/refresh` | 토큰 갱신 |
| `GET /actuator/health` | 서버 상태 확인 |
| `GET /actuator/info` | 서버 정보 조회 |
| `GET /swagger-ui/**` | Swagger UI |
| `GET /v3/api-docs/**` | OpenAPI Docs |
| `POST /ses/feedback/**` | AWS SNS 콜백 (레거시) |
| `POST /ses/callback/**` | SES 이벤트 콜백 (Callback Secret 검증만 적용) |

---

## Auth API

### POST /auth/login
로그인

**Request:**
```json
{ "username": "admin", "password": "admin" }
```

**Response (200):**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": { "userId": 1, "username": "admin", "displayName": "관리자", "role": "ADMIN" }
}
```

### POST /auth/refresh
토큰 갱신

**Request:**
```json
{ "refreshToken": "eyJ..." }
```

**Response (200):**
```json
{ "accessToken": "eyJ..." }
```

### POST /auth/change-password
비밀번호 변경 (JWT 인증 필요)

**Request:**
```json
{ "currentPassword": "oldpass", "newPassword": "newpass" }
```

### GET /users/me
현재 로그인 사용자 정보 (JWT 인증 필요)

### GET /users
사용자 목록 조회 (JWT 인증 필요)

### POST /users
사용자 생성 (JWT 인증 필요)

**Request:**
```json
{ "username": "newuser", "password": "pass1234", "displayName": "새 사용자", "role": "ADMIN" }
```

### PUT /users/{userId}
사용자 수정

### DELETE /users/{userId}
사용자 삭제

---

## 1. AWS SES - 이메일 발송

> 모든 발송 요청은 `ApiGatewayClient`를 통해 API Gateway로 전달됩니다.
> 발송 전 `SenderValidationService`가 `from` 주소를 `TENANT_SENDER` 테이블에서 검증합니다.

### 1.1 텍스트/HTML 이메일 발송

```
POST /ses/text-mail
```

**Request Body**
```json
{
  "from": "sender@mycompany.com",
  "to": "recipient@example.com",
  "subject": "이메일 제목",
  "body": "<h1>HTML 본문</h1>",
  "tags": [
    { "name": "campaign", "value": "welcome-email" }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| from | String (Email) | Y | 발신자 이메일 (TENANT_SENDER 등록 필요) |
| to | String (Email) | Y | 수신자 이메일 |
| subject | String | Y | 이메일 제목 |
| body | String | Y | HTML 본문 |
| tags | List\<MessageTag\> | N | 추적용 태그 |

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
  "from": "sender@mycompany.com",
  "to": ["recipient1@example.com", "recipient2@example.com"],
  "cc": ["cc@example.com"],
  "bcc": ["bcc@example.com"],
  "templateData": {
    "user_name": "홍길동",
    "company": "MyCompany"
  },
  "tags": [
    { "name": "campaign", "value": "welcome" }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| templateName | String | Y | SES 템플릿 이름 |
| from | String | Y | 발신자 이메일 (TENANT_SENDER 등록 필요) |
| to | List\<String\> | Y | 수신자 목록 |
| cc | List\<String\> | N | 참조 목록 |
| bcc | List\<String\> | N | 숨은참조 목록 |
| templateData | Map\<String, String\> | Y | 템플릿 변수 데이터 |
| tags | List\<MessageTag\> | N | 추적용 태그 |

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

> API Gateway `POST /tenant-setup` (action: CREATE_TEMPLATE) 경유

**Request Body**
```json
{
  "templateName": "Welcome-Template",
  "subjectPart": "안녕하세요, {{user_name}}님",
  "htmlPart": "<h1>Welcome {{user_name}}</h1>",
  "textPart": "Welcome {{user_name}}"
}
```

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

> API Gateway `POST /tenant-setup` (action: UPDATE_TEMPLATE) 경유

**Request/Response**: 1.3 템플릿 생성과 동일

---

### 1.5 템플릿 삭제

```
DELETE /ses/template
```

> API Gateway `POST /tenant-setup` (action: DELETE_TEMPLATE) 경유

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

> API Gateway `GET /tenant-setup?action=LIST_TEMPLATES` 경유

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

## 2. Tenant - 테넌트 관리

### 2.1 테넌트 생성

```
POST /tenant
```

**Request Body**
```json
{
  "tenantName": "MyCompany",
  "domain": "mycompany.com"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| tenantName | String | Y | 테넌트 이름 |
| domain | String | Y | 이메일 발송 도메인 |

**Response** `201 Created`
```json
{
  "tenantId": "uuid",
  "tenantName": "MyCompany",
  "domain": "mycompany.com",
  "apiKey": "generated-api-key",
  "configSetName": null,
  "verificationStatus": "PENDING",
  "quotaDaily": 0,
  "quotaMonthly": 0,
  "status": "PENDING",
  "createdAt": "2024-01-01T00:00:00"
}
```

---

### 2.2 테넌트 조회

```
GET /tenant/{tenantId}
```

**Response** `200 OK`: 2.1 Response와 동일

---

### 2.3 테넌트 목록 조회

```
GET /tenant/list?status={status}&page={page}&size={size}
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| status | String | N | - | 상태 필터 (ACTIVE, INACTIVE, PENDING) |
| page | int | N | 0 | 페이지 번호 |
| size | int | N | 20 | 페이지 크기 |

**Response** `200 OK`
```json
{
  "totalCount": 10,
  "tenants": [...]
}
```

---

### 2.4 테넌트 수정

```
PATCH /tenant/{tenantId}
```

**Request Body**
```json
{
  "tenantName": "Updated Name",
  "quotaDaily": 1000,
  "quotaMonthly": 30000
}
```

**Response** `200 OK`: 2.1 Response와 동일

---

### 2.5 테넌트 비활성화

```
DELETE /tenant/{tenantId}
```

**Response** `204 No Content`

---

### 2.6 테넌트 영구 삭제

```
DELETE /tenant/{tenantId}/permanent
```

비활성 상태의 테넌트만 영구 삭제 가능합니다.

**Response** `204 No Content`

---

### 2.7 테넌트 활성화

```
POST /tenant/{tenantId}/activate
```

**Response** `200 OK`

---

### 2.8 API 키 재발급

```
POST /tenant/{tenantId}/regenerate-key
```

**Response** `200 OK`: 2.1 Response와 동일 (새 API Key 포함)

---

### 2.9 할당량 사용 현황 조회

```
GET /tenant/{tenantId}/quota
```

**Response** `200 OK`
```json
{
  "tenantId": "uuid",
  "daily": {
    "limit": 1000,
    "used": 150,
    "remaining": 850
  },
  "monthly": {
    "limit": 30000,
    "used": 5000,
    "remaining": 25000
  }
}
```

---

### 2.10 할당량 수정

```
PATCH /tenant/{tenantId}/quota
```

**Request Body**
```json
{
  "quotaDaily": 2000,
  "quotaMonthly": 60000
}
```

**Response** `200 OK`: 2.1 Response와 동일

---

### 2.11 발신자 이메일 목록 조회

```
GET /tenant/{tenantId}/senders
```

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "tenantId": "uuid",
    "email": "no-reply@mycompany.com",
    "displayName": "MyCompany",
    "isDefault": true,
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

---

### 2.12 발신자 이메일 등록

```
POST /tenant/{tenantId}/senders
```

도메인 검증: 발신자 이메일은 테넌트 도메인(`@{domain}`)과 일치해야 합니다.

**Request Body**
```json
{
  "email": "no-reply@mycompany.com",
  "displayName": "MyCompany",
  "isDefault": true
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "tenantId": "uuid",
  "email": "no-reply@mycompany.com",
  "displayName": "MyCompany",
  "isDefault": true,
  "createdAt": "2024-01-01T00:00:00"
}
```

**Error** `400 Bad Request` (도메인 불일치)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "발신자 이메일은 테넌트 도메인(@mycompany.com)만 허용됩니다."
}
```

---

### 2.13 발신자 이메일 삭제

```
DELETE /tenant/{tenantId}/senders/{email}
```

**Response** `204 No Content`

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
  "from": "no-reply@mycompany.com",
  "templatedEmailList": [
    {
      "id": "email-001",
      "to": ["user1@example.com"],
      "cc": [],
      "bcc": [],
      "templateParameters": { "user_name": "홍길동" }
    }
  ],
  "tags": [
    { "name": "campaign", "value": "welcome-2024" }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| jobName | String | Y | 작업 이름 (고유값) |
| jobGroup | String | N | 작업 그룹 (기본: DEFAULT) |
| description | String | N | 작업 설명 |
| startDateAt | LocalDateTime | N | 예약 시간 (미래만 허용, 미지정 시 즉시 실행) |
| templateName | String | Y | SES 템플릿 이름 |
| from | String | Y | 발신자 이메일 |
| templatedEmailList | List\<TemplatedEmail\> | Y | 발송 대상 목록 |
| tags | List\<MessageTag\> | N | 추적용 태그 |

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

**Request/Response**: 3.3과 동일 형식

---

### 3.5 작업 중지

```
PUT /scheduler/job/stop
```

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

## 4. 보정 폴링 (내부 스케줄링)

외부 API 없이 내부적으로 동작하는 자동화 프로세스입니다.

### 4.1 신규 이메일 폴링

- **주기**: 60초
- **동작**: PostgreSQL에서 발송 대기 상태(`SR`)인 이메일을 최대 280건 조회 후 Quartz 스케줄러에 자동 등록
- **조회 범위**: 현재 시점으로부터 1주일 이내 데이터

### 4.2 발송 결과 보정 폴링

- **주기**: Settings에서 설정한 `pollingInterval` 값 (기본 300,000ms = 5분)
- **동작**: API Gateway `GET /results` → Lambda `ems-event-query` → DynamoDB `ems-send-results` 조회
- **처리**: 멱등성 처리로 PostgreSQL 상태 업데이트 (이미 최종 상태면 무시)
- **이벤트 유형**: Delivery (발송 완료), Bounce (반송), Complaint (수신 거부)

---

## 5. SES Callback - 이벤트 콜백

### 5.1 SES 이벤트 처리

```
POST /ses/callback/event
```

**보안**: `X-Callback-Secret` 헤더 검증 (SYSTEM_CONFIG의 `callback.secret`과 비교)
- Secret 미설정 시 검증 없이 통과
- 불일치 시 HTTP 401

**Request Body**
```json
{
  "tenantId": "uuid",
  "messageId": "aws-ses-message-id",
  "eventType": "DELIVERY",
  "timestamp": "2024-01-01T00:00:00",
  "recipients": ["user@example.com"],
  "details": {}
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| tenantId | String | Y | 테넌트 ID |
| messageId | String | Y | SES 메시지 ID |
| eventType | String | Y | 이벤트 유형 (DELIVERY, BOUNCE, COMPLAINT) |
| timestamp | LocalDateTime | N | 이벤트 발생 시간 |
| recipients | List\<String\> | N | 수신자 이메일 목록 |
| details | Map | N | 추가 상세 정보 |

**Response** `200 OK`
```json
{
  "result": true,
  "processed": 1
}
```

BOUNCE/COMPLAINT 이벤트 수신 시 해당 수신자를 자동으로 `SUPPRESSION_LIST`에 추가합니다.

---

### 5.2 콜백 상태 확인

```
GET /ses/callback/health
```

**Response** `200 OK`
```json
{
  "status": "UP",
  "lastEventTime": "2024-01-01T00:00:00"
}
```

---

## 6. Onboarding - 테넌트 온보딩

> 온보딩 시 ESM은 `ApiGatewayClient`를 통해 API Gateway `POST /tenant-setup`을 호출하여 AWS 리소스(SES Identity, ConfigSet, DynamoDB ems-tenant-config)를 자동 구성합니다.

### 6.1 온보딩 시작 (도메인 인증 방식)

```
POST /onboarding/start
```

테넌트를 생성하고 API Gateway를 통해 SES 도메인 아이덴티티를 등록합니다.

**Request Body**
```json
{
  "tenantName": "MyCompany",
  "domain": "mycompany.com",
  "contactEmail": "admin@mycompany.com"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| tenantName | String | Y | 테넌트 이름 |
| domain | String | Y | 이메일 발송 도메인 |
| contactEmail | String | N | 담당자 이메일 |

**Response** `201 Created`
```json
{
  "tenant": {
    "tenantId": "uuid",
    "tenantName": "MyCompany",
    "domain": "mycompany.com",
    "apiKey": "generated-api-key",
    "status": "PENDING"
  },
  "dkimRecords": {
    "domain": "mycompany.com",
    "verificationStatus": "PENDING",
    "dkimRecords": [
      {
        "name": "token._domainkey.mycompany.com",
        "type": "CNAME",
        "value": "token.dkim.amazonses.com"
      }
    ]
  }
}
```

---

### 6.2 온보딩 상태 조회

```
GET /onboarding/{tenantId}/status
```

**Response** `200 OK`
```json
{
  "tenantId": "uuid",
  "domain": "mycompany.com",
  "steps": [
    { "step": 1, "name": "테넌트 생성", "status": "COMPLETED" },
    { "step": 2, "name": "도메인 인증", "status": "WAITING" },
    { "step": 3, "name": "ConfigSet 구성", "status": "PENDING" },
    { "step": 4, "name": "테넌트 활성화", "status": "PENDING" }
  ],
  "verificationStatus": "PENDING",
  "tenantStatus": "PENDING"
}
```

온보딩 Step Status: `COMPLETED`, `WAITING`, `PENDING`

---

### 6.3 DKIM 레코드 조회

```
GET /onboarding/{tenantId}/dkim
```

**Response** `200 OK`
```json
{
  "domain": "mycompany.com",
  "verificationStatus": "SUCCESS",
  "dkimRecords": [
    {
      "name": "token._domainkey.mycompany.com",
      "type": "CNAME",
      "value": "token.dkim.amazonses.com"
    }
  ]
}
```

verificationStatus: `PENDING` (대기), `SUCCESS` (인증 완료), `FAILED` (실패)

---

### 6.4 테넌트 수동 활성화

```
POST /onboarding/{tenantId}/activate
```

ConfigSet을 생성하고 DynamoDB `ems-tenant-config`에 등록 후 테넌트 상태를 ACTIVE로 변경합니다.

**Response** `200 OK`: Tenant 응답 DTO (2.1 형식)

---

### 6.5 이메일 개별 인증 요청

```
POST /onboarding/{tenantId}/verify-email
```

DNS 접근 불가 시 개별 이메일 주소를 SES에 등록합니다. SES가 인증 이메일을 자동 발송합니다.

**Request Body**
```json
{
  "email": "user@example.com"
}
```

**Response** `201 Created`
```json
{
  "email": "user@example.com",
  "verificationStatus": "PENDING"
}
```

---

### 6.6 이메일 인증 상태 조회

```
GET /onboarding/{tenantId}/email-status/{email}
```

**Response** `200 OK`
```json
{
  "email": "user@example.com",
  "verificationStatus": "SUCCESS"
}
```

verificationStatus: `PENDING` (대기), `SUCCESS` (인증 완료), `FAILED` (실패)

---

### 6.7 인증 이메일 재발송

```
POST /onboarding/{tenantId}/resend-verification/{email}
```

기존 SES Identity를 삭제하고 재생성하여 인증 이메일을 재발송합니다.

**Response** `200 OK`
```json
{
  "email": "user@example.com",
  "verificationStatus": "PENDING"
}
```

---

## 7. SES Identity - 도메인 아이덴티티 관리

> API Gateway `POST /tenant-setup` 경유 (action 파라미터로 구분)

### 7.1 도메인 아이덴티티 생성

```
POST /ses/identity
```

**Request Body**
```json
{
  "domain": "mycompany.com"
}
```

**Response** `200 OK`: DKIM Records DTO (6.3 응답과 동일)

---

### 7.2 도메인 인증 상태 조회

```
GET /ses/identity/{domain}
```

**Response** `200 OK`: DKIM Records DTO

---

### 7.3 도메인 아이덴티티 삭제

```
DELETE /ses/identity/{domain}
```

**Response** `204 No Content`

---

## 8. SES ConfigSet - 구성 세트 관리

> API Gateway `POST /tenant-setup` 경유

### 8.1 ConfigSet 생성

```
POST /ses/config-set
```

**Request Body**
```json
{
  "tenantId": "uuid"
}
```

**Response** `201 Created`
```json
{
  "configSetName": "tenant-uuid"
}
```

---

### 8.2 ConfigSet 조회

```
GET /ses/config-set/{tenantId}
```

**Response** `200 OK`
```json
{
  "configSetName": "tenant-uuid",
  "tenantId": "uuid"
}
```

---

### 8.3 ConfigSet 삭제

```
DELETE /ses/config-set/{tenantId}
```

**Response** `204 No Content`

---

## 9. Suppression - 수신 거부 목록

### 9.1 수신 거부 목록 조회

```
GET /suppression/tenant/{tenantId}?page={page}&size={size}
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | int | N | 0 | 페이지 번호 |
| size | int | N | 20 | 페이지 크기 |

**Response** `200 OK`
```json
{
  "totalCount": 5,
  "suppressions": [
    {
      "id": 1,
      "tenantId": "uuid",
      "email": "bounced@example.com",
      "reason": "BOUNCE",
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

Suppression Reason: `BOUNCE`, `COMPLAINT`

---

### 9.2 수신 거부 제거

```
DELETE /suppression/tenant/{tenantId}/{email}
```

**Response** `204 No Content`

---

## 10. Settings - 시스템 설정

### 아키텍처 개요

```
ESM 설정 저장 시:
  ① ESM DB (SYSTEM_CONFIG) 저장 — 모든 설정
  ② API Gateway PUT /config 호출 (ApiGatewayClient)
     → Lambda ems-config-updater → SSM Parameter Store 동기화
  → Lambda ems-event-processor가 SSM 캐시(30초) 만료 후 자동 반영
```

### 설정 분류

| 구분 | 항목 | 저장 위치 |
|------|------|-----------|
| API Gateway 연결 | Endpoint URL, 리전, 인증 방식, API Key | ESM DB (SYSTEM_CONFIG) |
| API Gateway 경로 | /send-email, /results, /config, /tenant-setup | ESM DB (SYSTEM_CONFIG) |
| SSM 동기화 대상 | Callback URL, Callback Secret, 수신 모드, 폴링 주기 | ESM DB + SSM |

---

### 10.1 설정 조회

```
GET /settings/aws
```

**Response** `200 OK`
```json
{
  "gatewayEndpoint": "https://xxx.execute-api.ap-northeast-2.amazonaws.com/prod",
  "gatewayRegion": "ap-northeast-2",
  "gatewayAuthType": "API_KEY",
  "gatewayApiKeyMasked": "abcd****",
  "gatewayAccessKey": "",
  "gatewaySecretKeyMasked": "",
  "gatewaySendPath": "/send-email",
  "gatewayResultsPath": "/results",
  "gatewayConfigPath": "/config",
  "gatewayConfigured": true,
  "callbackUrl": "https://esm-server/ses/callback/event",
  "callbackSecretMasked": "abcd****",
  "callbackConfigured": true,
  "deliveryMode": "callback",
  "pollingInterval": "300000",
  "updatedAt": "2024-01-01T00:00:00"
}
```

---

### 10.2 설정 저장

```
PUT /settings/aws
```

저장 시 ESM DB 저장 + API Gateway `PUT /config` 호출로 SSM 동기화합니다.

**Request Body**
```json
{
  "gatewayEndpoint": "https://xxx.execute-api.ap-northeast-2.amazonaws.com/prod",
  "gatewayRegion": "ap-northeast-2",
  "gatewayAuthType": "API_KEY",
  "gatewayApiKey": "your-api-key",
  "gatewayAccessKey": "",
  "gatewaySecretKey": "",
  "gatewaySendPath": "/send-email",
  "gatewayResultsPath": "/results",
  "gatewayConfigPath": "/config",
  "gatewayTenantSetupPath": "/tenant-setup",
  "callbackUrl": "https://esm-server/ses/callback/event",
  "callbackSecret": "your-secret",
  "deliveryMode": "callback",
  "pollingInterval": "300000"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| gatewayEndpoint | String | Y | API Gateway Base URL |
| gatewayRegion | String | Y | AWS 리전 |
| gatewayAuthType | String | Y | 인증 방식: `API_KEY` 또는 `IAM` |
| gatewayApiKey | String | N | API Gateway API Key |
| gatewaySendPath | String | N | 발송 경로 (기본: `/send-email`) |
| gatewayResultsPath | String | N | 결과 조회 경로 (기본: `/results`) |
| gatewayConfigPath | String | N | 설정 경로 (기본: `/config`) |
| gatewayTenantSetupPath | String | N | 온보딩/템플릿 경로 (기본: `/tenant-setup`) |
| callbackUrl | String | N | Lambda → ESM 콜백 URL |
| callbackSecret | String | N | 콜백 무결성 검증 시크릿 (X-Callback-Secret) |
| deliveryMode | String | Y | `callback` 또는 `polling` |
| pollingInterval | String | N | 보정 폴링 주기 (ms, 기본: 300000) |

**Response** `200 OK`: 10.1 응답과 동일

---

### 10.3 API Gateway 연결 테스트

```
POST /settings/aws/test
```

**Request Body**: 10.2와 동일

**Response** `200 OK`
```json
{
  "connected": true,
  "message": "API Gateway 연결 성공 (HTTP 200)",
  "statusCode": 200
}
```

---

### 10.4 발송 결과 수신 모드

| 모드 | Lambda 동작 | ESM 동작 | 사용 상황 |
|------|------------|---------|-----------|
| `callback` | DynamoDB 저장 + ESM 콜백 호출 | Callback 수신 + 보정 폴링 | 정상 운영 |
| `polling` | DynamoDB 저장만 | 보정 폴링만 | ESM 장애, 콜백 포트 미개방 |

모드 전환 시 SSM Parameter Store에 자동 동기화되며 Lambda가 30초 캐시 만료 후 반영합니다.

---

## 11. Database Schema

### 11.1 주요 테이블

| Table | Description |
|-------|-------------|
| `TENANT` | 테넌트 정보 (tenantId, domain, apiKey, status 등) |
| `TENANT_SENDER` | 테넌트별 허용 발신자 이메일 목록 |
| `ADM_EMAIL_SEND_MST` | 이메일 발송 마스터 (캠페인 단위) |
| `ADM_EMAIL_SEND_DTL` | 이메일 발송 상세 (수신자 단위, 상태 코드) |
| `SUPPRESSION_LIST` | 수신 거부 목록 (BOUNCE/COMPLAINT) |
| `SYSTEM_CONFIG` | 시스템 설정 (API Gateway 연결 정보 등) |
| `QRTZ_*` | Quartz 스케줄러 테이블 (PostgreSQL DB Store) |

### 11.2 이메일 상태 코드

| Code | Description |
|------|-------------|
| `SR` | 발송 대기 (Send Ready) |
| `SS` | 스케줄러 등록 완료 |
| `SE` | AWS SES 발송 완료 |
| `SD` | 최종 발송 완료 (Delivery) |
| `SB` | 반송 (Bounce) |
| `SC` | 수신 거부 (Complaint) |
| `SF` | 발송 실패 (Fail) |

### 11.3 발송 구분 코드

| Code | Description |
|------|-------------|
| `T` | 텍스트/HTML 이메일 |
| `P` | 템플릿 이메일 |

---

## 12. Configuration

### 12.1 Environment Variables

| Variable | Description | Dev Default |
|----------|-------------|-------------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/esm` |
| `DB_USERNAME` | PostgreSQL 사용자명 | `esm` |
| `DB_PASSWORD` | PostgreSQL 비밀번호 | `esm` |
| `QUARTZ_DB_URL` | Quartz용 PostgreSQL URL | `jdbc:postgresql://localhost:5432/esm` |
| `QUARTZ_DB_USER` | Quartz용 사용자명 | `esm` |
| `QUARTZ_DB_PASSWORD` | Quartz용 비밀번호 | `esm` |
| `API_KEY` | 레거시 단일 API 인증 키 | (미설정 시 인증 비활성화) |

> AWS Access Key / Secret Key는 사용하지 않습니다. 모든 AWS 연동은 API Gateway 경유입니다.

### 12.2 Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `7092` | 서버 포트 |
| `spring.lifecycle.timeout-per-shutdown-phase` | `30s` | Graceful shutdown 대기 시간 |
| `polling.schedule.send-email-check-time` | `60000` | 신규 이메일 폴링 주기 (ms) |
| `polling.schedule.send-email-event-check-time` | `60000` | 발송 결과 보정 폴링 내부 체크 주기 (ms) |
| `spring.quartz.threadPool.threadCount` | `10` | Quartz 스레드 풀 크기 |
| `security.api-key` | (환경변수) | 레거시 단일 API Key |

### 12.3 Profiles

| Profile | Description |
|---------|-------------|
| `local` | 로컬 개발 환경 |
| `dev` | 개발 서버 (외부 설정: `/svc/ems/config/ems-config-dev.yml`) |
| `prod` | 운영 서버 (외부 설정: `/svc/ems/config/ems-config-prod.yml`) |

---

## 13. Error Response

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
| `400` | 잘못된 요청 (Validation 실패, 도메인 불일치, 중복 작업 등) |
| `401` | 인증 실패 (API Key 누락/불일치, Callback Secret 불일치) |
| `500` | 서버 내부 오류 (API Gateway 연동 실패, DB 오류 등) |
