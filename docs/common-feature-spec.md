# Common Feature Specification

Backend와 Frontend에서 공통으로 적용되는 기능 명세입니다.

---

## 1. 인증 (Authentication)

### 1.0 JWT 인증 (Admin Dashboard)

- **방식**: JWT (JSON Web Token) 기반 인증
- **로그인**: `POST /auth/login` → `{ accessToken, refreshToken, user }`
- **토큰 갱신**: `POST /auth/refresh` → `{ accessToken }`
- **Access Token 유효기간**: 30분
- **Refresh Token 유효기간**: 7일
- **비활동 자동 로그아웃**: 30분 (Frontend AuthGuard)
- **필터 체인**: `JwtAuthenticationFilter` → `ApiKeyAuthenticationFilter` → `TenantContextFilter`
- **JWT 형식 판별**: Authorization 헤더의 토큰에 `.`이 2개 포함되면 JWT로 판별
- **초기 계정**: `admin` / `admin` (BCrypt 해시)

### Public Endpoints (인증 불필요)

| Endpoint | Description |
|----------|-------------|
| `POST /auth/login` | 로그인 |
| `POST /auth/refresh` | 토큰 갱신 |
| `GET /actuator/health` | 서버 상태 확인 |
| `GET /actuator/info` | 서버 정보 조회 |
| `GET /swagger-ui/**` | Swagger UI |
| `GET /v3/api-docs/**` | OpenAPI Docs |

### 1.1 Tenant API Key 인증

- **방식**: HTTP Header 기반 API Key 인증
- **Header**: `Authorization: {API_KEY}` 또는 `Authorization: Bearer {API_KEY}`
- **동작**: `ApiKeyAuthenticationFilter`가 DB(`TENANT` 테이블)에서 API Key를 조회하여 해당 테넌트를 식별
- **인증 성공 시**: `TenantContext`(ThreadLocal)에 tenantId 자동 설정
- **요청 완료 후**: `TenantContextFilter`가 ThreadLocal 정리

### 1.2 레거시 단일 API Key (하위 호환)

- 환경변수 `API_KEY`에 단일 키 설정 시, 테넌트 DB 조회 없이 인증 허용
- 이 경우 TenantContext는 `"default"`로 설정됨
- `API_KEY` 미설정 시 인증 완전 비활성화 (개발/테스트용)

### 1.3 Public Endpoints (인증 불필요)

| Endpoint | Description |
|----------|-------------|
| `POST /auth/login` | 로그인 |
| `POST /auth/refresh` | 토큰 갱신 |
| `GET /actuator/health` | 서버 상태 확인 |
| `GET /actuator/info` | 서버 정보 조회 |
| `GET /swagger-ui/**` | Swagger UI |
| `GET /v3/api-docs/**` | OpenAPI Docs |

---

## 2. 에러 처리 (Error Handling)

### 공통 응답 형식 (ApiResponse)

**성공 응답:**
```json
{
  "success": true,
  "data": { ... }
}
```

**에러 응답:**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값 검증에 실패했습니다.",
    "details": {
      "fieldName": "에러 메시지"
    }
  }
}
```

### 에러 코드

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Bean Validation 실패 (필드별 상세 에러 포함) |
| `BAD_REQUEST` | 400 | 잘못된 요청 파라미터 |
| `SCHEDULER_ERROR` | 500 | Quartz 스케줄러 오류 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

### HTTP Status Code

| Code | Description | 사용 예시 |
|------|-------------|-----------|
| `200` | 성공 | 조회, 수정 성공 |
| `201` | 생성 성공 | 테넌트 생성, 예약 작업 생성 |
| `204` | 삭제 성공 | 테넌트 비활성화, 수신 거부 제거 |
| `400` | 잘못된 요청 | Validation 실패, 도메인 불일치, 중복 작업 |
| `401` | 인증 실패 | API Key 누락/불일치 |
| `500` | 서버 오류 | API Gateway 연동 실패, DB 오류 |

---

## 3. Validation

### 이메일 주소 검증
- `@Email` 어노테이션 기반 형식 검증
- 발신자(`from`), 수신자(`to`) 필수 검증

### 발신자 이메일 도메인 검증
- 이메일 발송 시 `SenderValidationService`가 `from` 주소를 `TENANT_SENDER` 테이블에서 조회
- 테넌트에 등록되지 않은 발신자 이메일로 발송 시 예외 발생
- 발신자 이메일 등록 시 테넌트 도메인과 일치해야 함

### 필수값 검증
- `@NotNull`, `@NotEmpty` 기반 필수 필드 검증
- 템플릿 이름, 발신자, 수신자, 태그 등

### 스케줄 시간 검증
- `@Future` 어노테이션으로 과거 시간 설정 방지
- `yyyy-MM-dd'T'HH:mm:ss` 형식

---

## 4. 로깅 (Logging)

### HTTP 요청/응답 로깅
- `HttpLogInterceptor`를 통한 모든 요청/응답 자동 로깅
- 로깅 항목: Method, URI, Headers, Body, Remote IP, 처리 시간

### 환경별 로깅 설정

| Environment | Log Level | 출력 |
|-------------|-----------|------|
| local | DEBUG | Console |
| dev | DEBUG | Console + File |
| prod | INFO | Console + File |

---

## 5. 이메일 상태 관리

### 상태 흐름도

```
Queued (발송 큐 진입)
 └→ Sending (SES API 호출 완료)
     ├→ Delivered (수신 MTA 전달 확인)
     ├→ Bounced (반송)
     ├→ Complained (수신자 스팸 신고)
     ├→ Rejected (SES 발송 거부)
     ├→ Delayed (일시적 전달 지연)
     │   └→ Delivered / Bounced / ...
     ├→ Error (시스템 오류)
     ├→ Blocked (내부 차단)
     └→ Timeout (1시간 초과 자동 전환)
```

### 상태 코드 정의 (EnumEmailSendStatusCode)

| Code | Description | Terminal |
|------|-------------|----------|
| `SQ` | 스케줄러 큐 진입 (레거시) | - |
| `Queued` | 발송 큐 진입 (Quartz 스케줄링 완료) | - |
| `Sending` | SES API 호출 완료, 결과 대기 중 | - |
| `Delayed` | 일시적 전달 지연 (DeliveryDelay 이벤트) | - |
| `Delivered` | 수신 MTA 전달 확인 | Yes |
| `Bounced` | 반송 (Bounce 이벤트) | Yes |
| `Complained` | 수신자 스팸 신고 (Complaint 이벤트) | Yes |
| `Rejected` | SES 발송 거부 (Reject 이벤트) | Yes |
| `Error` | 시스템 오류 (RenderingFailure/SESFail/QuartzFail) | Yes |
| `Blocked` | 내부 차단 (Blacklist/Suppression) | Yes |
| `Timeout` | SES 이벤트 미도달 타임아웃 (1시간 초과 Sending → 자동 전환) | Yes |

---

## 6. 발송 구분 코드

| Code | Type | Description |
|------|------|-------------|
| `T` | Text | 텍스트/HTML 이메일 |
| `P` | Template | 템플릿 기반 이메일 |

---

## 7. 메시지 태그 (Message Tag)

모든 이메일 발송 시 추적용 태그를 포함합니다.

```json
{
  "tags": [
    { "name": "campaign", "value": "welcome-2024" },
    { "name": "event", "value": "signup" }
  ]
}
```

- AWS SES 메시지 태그와 매핑
- 캠페인/이벤트 단위 발송 추적 가능

---

## 8. 발송 결과 수신 모드

Phase 2부터 Callback 모드가 제거되고 EventBridge → DynamoDB → Polling 단일 경로로 동작합니다.

- **Lambda**: SES 이벤트 → EventBridge → `ems-event-processor` → DynamoDB `ems-send-results` 저장
- **ESM**: `ResultPollingService`가 주기적으로 DynamoDB를 폴링하여 로컬 DB 상태 보정

### 폴링 설정

| 항목 | 기본값 | 설명 |
|------|--------|------|
| 신규 이메일 폴링 주기 | 60,000ms (1분) | PostgreSQL 대기 이메일 조회 |
| 신규 이메일 조회 건수 | 280건 | 1회 폴링당 최대 처리 건수 |
| 발송 결과 보정 폴링 주기 | 120,000ms (2분) | API Gateway → DynamoDB 조회, `/settings/polling-interval`로 1~10분 설정 가능 |
| 발송 결과 조회 건수 | 300건 | 1회 폴링당 최대 처리 건수 |

---

## 9. Rate Limiting

- Phase 3부터 Guava `RateLimiter`가 제거되었습니다.
- Rate limiting은 SQS 동시성 제어 + SES 할당량 동적 조정으로 처리됩니다.
  - **SQS 동시성**: Lambda `ems-email-sender`의 Reserved Concurrency로 초당 발송 속도 제어
  - **SES 할당량**: `QuotaService.checkQuota(tenantId, count)`로 테넌트별 일별/월별 한도 검증 후 발송 큐 등록

---

## 10. 수신 거부 관리 (Suppression)

### 자동 등록
- 보정 폴링에서 BOUNCE/COMPLAINT 이벤트 감지 시 자동으로 수신 거부 목록에 추가
- 이미 등록된 이메일은 중복 추가하지 않음 (`TENANT_ID + EMAIL` UNIQUE 제약)

### 수신 거부 사유

| Reason | Description |
|--------|-------------|
| `BOUNCE` | 반송 (잘못된 주소 등) |
| `COMPLAINT` | 수신자가 스팸 신고 |

### 테이블
- `SUPPRESSION_LIST` (tenant_id + email UNIQUE 제약, tenant_id 인덱스)

---

## 11. 테넌트 할당량 (Quota)

- 테넌트별 일별/월별 이메일 발송 한도 설정 가능
- `QuotaService`에서 실시간 사용량 조회 및 초과 여부 확인
- 미설정 시 무제한 (Integer.MAX_VALUE)

---

## 12. 테넌트 온보딩 워크플로우

### 인증 방식 1: 도메인 인증 (DKIM)

도메인 DNS 관리 권한이 있는 경우 권장합니다.

| Step | Name | 완료 조건 |
|------|------|-----------|
| 1 | 테넌트 생성 | 테넌트 레코드 생성 |
| 2 | 도메인 인증 | SES 도메인 아이덴티티 등록 + DNS CNAME 추가 → SUCCESS |
| 3 | ConfigSet 구성 | ConfigSet 생성 및 DynamoDB 등록 |
| 4 | 테넌트 활성화 | 상태 ACTIVE 전환 |

### 인증 방식 2: 이메일 개별 인증

DNS 접근이 불가한 경우 이메일 주소 단위로 인증합니다.

| Step | Name | 완료 조건 |
|------|------|-----------|
| 1 | 테넌트 생성 | 테넌트 레코드 생성 |
| 2 | 이메일 인증 요청 | SES 이메일 아이덴티티 등록 → 인증 메일 수신 |
| 3 | 인증 메일 확인 | 수신자가 링크 클릭 → SUCCESS |
| 4 | ConfigSet 구성 + 활성화 | 수동 활성화 |

### 테넌트 상태

| Status | Description |
|--------|-------------|
| `PENDING` | 온보딩 진행 중 |
| `ACTIVE` | 활성 (이메일 발송 가능) |
| `INACTIVE` | 비활성화 |

---

## 13. 발신자 이메일 관리 (TENANT_SENDER)

- 테넌트별로 허용된 발신자 이메일 주소 목록을 `TENANT_SENDER` 테이블에서 관리
- 등록 시 테넌트에 설정된 도메인과 이메일 도메인이 일치해야 함
  - 예: 테넌트 도메인 `mycompany.com` → `no-reply@mycompany.com` 허용
- 이메일 발송 API 호출 시 `from` 주소가 해당 테넌트의 TENANT_SENDER에 없으면 400 에러

---

## 14. Settings - API Gateway 설정

### 설정 분류

| 구분 | 항목 | 저장 위치 |
|------|------|-----------|
| API Gateway 연결 | Endpoint URL, 리전, 인증 방식, API Key | ESM DB (SYSTEM_CONFIG) |
| 경로 설정 | /send-email, /results, /tenant-setup | ESM DB (SYSTEM_CONFIG) |
| 폴링 설정 | 발송 결과 보정 폴링 주기 (기본 2분, 1~10분) | ESM DB (SYSTEM_CONFIG) |

### 설정 흐름

```
PUT /settings/aws
  → ESM DB (SYSTEM_CONFIG) 저장

PUT /settings/polling-interval
  → ESM DB (SYSTEM_CONFIG) 저장
  → ResultPollingService 즉시 반영
```

---

## 15. Graceful Shutdown

- 서버 종료 시 진행 중인 작업 완료 후 종료
- Shutdown timeout: 30초 (`spring.lifecycle.timeout-per-shutdown-phase: 30s`)
- Quartz 스케줄러: 실행 중인 작업 완료 대기 후 종료 (`wait-for-jobs-to-complete-on-shutdown: true`)
