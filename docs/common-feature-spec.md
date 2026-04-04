# Common Feature Specification

Backend와 Frontend에서 공통으로 적용되는 기능 명세입니다.

---

## 1. 인증 (Authentication)

### API Key 인증
- **방식**: HTTP Header 기반 API Key 인증
- **Header**: `Authorization: {API_KEY}`
- **미설정 시**: API Key가 환경변수에 설정되지 않으면 인증 비활성화

### 테넌트 API Key 인증
- 멀티테넌트 환경에서는 각 테넌트별 API Key로 인증
- 인증 성공 시 `TenantContext`에 테넌트 정보가 자동 설정됨
- `TenantContextFilter`가 요청 완료 후 ThreadLocal을 정리

### Public Endpoints (인증 불필요)
| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | 서버 상태 확인 |
| `GET /actuator/info` | 서버 정보 조회 |
| `POST /ses/feedback/**` | AWS SNS 콜백 수신 |
| `POST /ses/callback/**` | SES 이벤트 콜백 수신 |

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
| `200` | 성공 | 조회, 수정, 삭제 성공 |
| `201` | 생성 성공 | 스케줄 작업 생성 |
| `400` | 잘못된 요청 | Validation 실패, 중복 작업 |
| `401` | 인증 실패 | API Key 누락/불일치 |
| `500` | 서버 오류 | AWS 연동 실패, DB 오류 |

---

## 3. Validation

### 이메일 주소 검증
- `@Email` 어노테이션 기반 형식 검증
- 발신자(from), 수신자(to) 필수 검증

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

### Logback Appender
- Slack Appender: (예정)

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
| `SD` | Delivered | 수신자에게 정상 전달 |
| `SB` | Bounced | 반송 (잘못된 주소 등) |
| `SC` | Complained | 수신자가 스팸 신고 |
| `SF` | Failed | 발송 실패 |

---

## 6. 발송 구분 코드

| Code | Type | Description |
|------|------|-------------|
| `T` | Text | 텍스트 이메일 |
| `H` | HTML | HTML 이메일 |
| `P` | Template | 템플릿 기반 이메일 |

---

## 7. 메시지 태그 (Message Tag)

모든 이메일 발송 시 추적용 태그를 필수로 포함합니다.

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

## 8. Polling 설정

| 항목 | 기본값 | 설명 |
|------|--------|------|
| 신규 이메일 폴링 주기 | 60,000ms (1분) | PostgreSQL 대기 이메일 조회 |
| 신규 이메일 조회 건수 | 280건 | 1회 폴링당 최대 처리 건수 |
| 발송 결과 폴링 주기 | 60,000ms (1분) | DynamoDB SES 이벤트 조회 |
| 발송 결과 조회 건수 | 300건 | 1회 폴링당 최대 처리 건수 |
| 조회 범위 | 최근 1주일 | 이메일 데이터 조회 기간 |

---

## 9. Rate Limiting

- Guava `RateLimiter` 적용
- AWS SES 발송 제한 준수 (SES 계정별 초당 발송 한도)

---

## 10. 수신 거부 관리 (Suppression)

### 자동 등록
- SES Callback에서 BOUNCE/COMPLAINT 이벤트 수신 시 자동으로 수신 거부 목록에 추가
- 이미 등록된 이메일은 중복 추가하지 않음

### 수신 거부 사유
| Reason | Description |
|--------|-------------|
| `BOUNCE` | 반송 (잘못된 주소 등) |
| `COMPLAINT` | 수신자가 스팸 신고 |

### 테이블
- `SUPPRESSION_LIST` (tenant_id + email UNIQUE 제약)

---

## 11. 테넌트 할당량 (Quota)

- 테넌트별 일별/월별 이메일 발송 한도 설정 가능
- `QuotaService`에서 실시간 사용량 조회 및 초과 여부 확인
- 미설정 시 무제한 (Integer.MAX_VALUE)

---

## 12. 테넌트 온보딩 워크플로우

### 온보딩 단계
| Step | Name | 완료 조건 |
|------|------|-----------|
| 1 | 테넌트 생성 | 테넌트 레코드 생성 |
| 2 | 도메인 인증 | SES 도메인 인증 SUCCESS |
| 3 | ConfigSet 구성 | ConfigSet 이름 설정됨 |
| 4 | 테넌트 활성화 | 상태 ACTIVE |

### 테넌트 상태
| Status | Description |
|--------|-------------|
| `PENDING` | 온보딩 진행 중 |
| `ACTIVE` | 활성 (이메일 발송 가능) |
| `INACTIVE` | 비활성화 |

---

## 13. Graceful Shutdown

- 서버 종료 시 진행 중인 작업 완료 후 종료
- Shutdown timeout: 30초
- Quartz 스케줄러: 실행 중인 작업 완료 대기 후 종료
