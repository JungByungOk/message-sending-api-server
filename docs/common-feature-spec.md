# Common Feature Specification

Backend와 Frontend에서 공통으로 적용되는 기능 명세입니다.

---

## 1. 인증 (Authentication)

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

### 1.3 Callback Secret 검증

- **대상**: `POST /ses/callback/**` 엔드포인트
- **Header**: `X-Callback-Secret: {secret}`
- **동작**: `CallbackSecretFilter`가 SYSTEM_CONFIG의 `callback.secret` 값과 비교
- **Secret 미설정 시**: 검증 없이 통과 (개발 환경)
- **불일치 시**: HTTP 401 반환

### 1.4 Public Endpoints (인증 불필요)

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | 서버 상태 확인 |
| `GET /actuator/info` | 서버 정보 조회 |
| `GET /swagger-ui/**` | Swagger UI |
| `GET /v3/api-docs/**` | OpenAPI Docs |
| `POST /ses/feedback/**` | AWS SNS 콜백 수신 (레거시) |
| `POST /ses/callback/**` | SES 이벤트 콜백 수신 (Callback Secret 검증만 적용) |

---

## 2. 에러 처리 (Error Handling)

### 공통 에러 응답 형식

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "상세 에러 메시지"
}
```

### HTTP Status Code

| Code | Description | 사용 예시 |
|------|-------------|-----------|
| `200` | 성공 | 조회, 수정 성공 |
| `201` | 생성 성공 | 테넌트 생성, 예약 작업 생성 |
| `204` | 삭제 성공 | 테넌트 비활성화, 수신 거부 제거 |
| `400` | 잘못된 요청 | Validation 실패, 도메인 불일치, 중복 작업 |
| `401` | 인증 실패 | API Key 누락/불일치, Callback Secret 불일치 |
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
SR (발송 대기)
 ├→ SS (스케줄러 등록)
 │   └→ SE (SES 발송 완료)
 │       ├→ SD (발송 성공 - Delivery)
 │       ├→ SB (반송 - Bounce)
 │       └→ SC (수신 거부 - Complaint)
 └→ SF (발송 실패)
```

### 상태 코드 정의

| Code | Name | Description |
|------|------|-------------|
| `SR` | Send Ready | 발송 대기 상태 (외부 시스템에서 INSERT) |
| `SS` | Scheduled | 스케줄러에 작업 등록 완료 |
| `SE` | Sent to SES | AWS SES로 발송 요청 완료 |
| `SD` | Delivered | 수신자에게 정상 전달 (Callback 또는 폴링으로 업데이트) |
| `SB` | Bounced | 반송 (잘못된 주소 등) |
| `SC` | Complained | 수신자가 스팸 신고 |
| `SF` | Failed | 발송 실패 |

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

| 모드 | Lambda 동작 | ESM 동작 | 사용 상황 |
|------|------------|---------|-----------|
| `callback` | DynamoDB 저장 + ESM `/ses/callback/event` 호출 | Callback 수신 + 보정 폴링 | 정상 운영 |
| `polling` | DynamoDB 저장만 | 보정 폴링만 | ESM 장애, 콜백 포트 미개방 |

### 보정 폴링 설정

| 항목 | 기본값 | 설명 |
|------|--------|------|
| 신규 이메일 폴링 주기 | 60,000ms (1분) | PostgreSQL 대기 이메일 조회 |
| 신규 이메일 조회 건수 | 280건 | 1회 폴링당 최대 처리 건수 |
| 발송 결과 보정 폴링 주기 | 설정값 (기본 300,000ms) | API Gateway → DynamoDB 조회 |
| 발송 결과 조회 건수 | 300건 | 1회 폴링당 최대 처리 건수 |

모드 전환 시 SSM Parameter Store에 자동 동기화되며, Lambda가 30초 캐시 만료 후 반영합니다.

---

## 9. Rate Limiting

- Guava `RateLimiter` 적용
- AWS SES 발송 제한 준수 (SES 계정별 초당 발송 한도)

---

## 10. 수신 거부 관리 (Suppression)

### 자동 등록
- Callback 수신 또는 보정 폴링에서 BOUNCE/COMPLAINT 이벤트 감지 시 자동으로 수신 거부 목록에 추가
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
| 경로 설정 | /send-email, /results, /config, /tenant-setup | ESM DB (SYSTEM_CONFIG) |
| SSM 동기화 대상 | Callback URL, Callback Secret, 수신 모드, 폴링 주기 | ESM DB + SSM (API Gateway 경유) |

### 설정 흐름

```
PUT /settings/aws
  → ESM DB (SYSTEM_CONFIG) 저장
  → API Gateway PUT /config 호출 (ApiGatewayClient)
  → Lambda ems-config-updater → SSM Parameter Store 업데이트
  → Lambda ems-event-processor가 SSM 캐시(30초) 만료 후 자동 반영
```

---

## 15. Graceful Shutdown

- 서버 종료 시 진행 중인 작업 완료 후 종료
- Shutdown timeout: 30초 (`spring.lifecycle.timeout-per-shutdown-phase: 30s`)
- Quartz 스케줄러: 실행 중인 작업 완료 대기 후 종료 (`wait-for-jobs-to-complete-on-shutdown: true`)
