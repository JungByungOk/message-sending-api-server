# Joins EMS — AWS 아키텍처

## 전체 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                  On-Premise IDC (목동)                        │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  ESM (Spring Boot + React)                            │   │
│  │                                                       │   │
│  │  ① POST /send-email    → 발송 요청                   │   │
│  │  ② GET  /results       → 보정 폴링 (5~10분)          │   │
│  │  ③ /ses/callback/event ← 실시간 수신                  │   │
│  │  ④ PUT  /config        → 설정 (UI에서 관리)           │   │
│  │  ⑤ POST /tenant-setup  → 테넌트 온보딩 (UI)          │   │
│  │     GET  /tenant-status → 인증 상태 확인              │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTPS (443)
                          │ x-api-key 인증 · IP Whitelist
┌─────────────────────────▼───────────────────────────────────┐
│                         AWS                                   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  API Gateway (ems-api)                               │    │
│  │  POST /send-email    → SQS                           │    │
│  │  GET  /results       → Lambda (event-query)          │    │
│  │  PUT  /config        → Lambda (config-updater) → SSM │    │
│  │  POST /tenant-setup  → Lambda (tenant-setup)          │    │
│  │  GET  /tenant-status → Lambda (tenant-setup)          │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─ 발송 경로 ─────────────────────────────────────────┐    │
│  │  SQS (ems-send-queue) → Lambda (email-sender) → SES │    │
│  │  └ DLQ (ems-send-dlq, 재시도 3회)                    │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─ 이벤트 수신 ───────────────────────────────────────┐    │
│  │  SES → SNS (ems-ses-events)                          │    │
│  │      → Lambda (event-processor)                       │    │
│  │          ① DynamoDB 저장 (항상)                       │    │
│  │          ② callback 모드 → ESM 콜백 호출             │    │
│  │             polling 모드 → 생략 (DB저장만)            │    │
│  │  └ DLQ (event-processor-dlq, 재시도 2회)             │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─ 보정 폴링 ─────────────────────────────────────────┐    │
│  │  API Gateway GET /results                             │    │
│  │      → Lambda (event-query) → DynamoDB GSI Query     │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─ 데이터 저장소 ─────────────────────────────────────┐    │
│  │  DynamoDB: ems-send-results (TTL 7일)                │    │
│  │  DynamoDB: ems-tenant-config (TTL 10년, 실질적 무제한) │    │
│  │  DynamoDB: ems-idempotency (TTL 24시간)              │    │
│  │  SSM: /ems/mode, /ems/callback_url, /ems/callback_secret │
│  └──────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Lambda 구성 (4+1개)

| 함수명 | 트리거 | 역할 | 주요 IAM 권한 |
|--------|--------|------|---------------|
| `ems-email-sender` | SQS | SES 이메일 발송, 멱등성 체크 | ses:SendEmail, dynamodb:GetItem/PutItem |
| `ems-event-processor` | SNS (직접) | 이벤트 → DynamoDB 저장 + 모드별 콜백 | dynamodb:PutItem, ssm:GetParameter |
| `ems-event-query` | API Gateway | DynamoDB 발송결과 조회 | dynamodb:Query |
| `ems-tenant-setup` | API Gateway | Identity/ConfigSet/템플릿 CRUD | ses:Create*, dynamodb:PutItem |
| `ems-config-updater` | API Gateway | SSM Parameter Store 업데이트 | ssm:PutParameter |

## DynamoDB 테이블

### ems-send-results
| Key | Type | 설명 |
|-----|------|------|
| `tenant_id` (PK) | String | 테넌트 ID |
| `message_id` (SK) | String | SES 메시지 ID |
| `status` | String | DELIVERED / BOUNCED / COMPLAINED |
| `event_type` | String | Delivery / Bounce / Complaint |
| `recipients` | StringSet | 수신자 이메일 목록 |
| `timestamp` | String | 이벤트 발생 시간 |
| `ttl` | Number | TTL (7일) |

**GSI**: `gsi-tenant-timestamp` (PK: tenant_id, SK: timestamp) — 보정 폴링 조회용

### ems-tenant-config
| Key | Type | 설명 |
|-----|------|------|
| `tenant_id` (PK) | String | 테넌트 ID |
| `tenant_name` | String | 테넌트명 |
| `domain` | String | 도메인 |
| `config_set_name` | String | SES Config Set 이름 |
| `verification_status` | String | PENDING / SUCCESS |
| `ttl` | Number | TTL (10년, 환경변수로 설정 가능) |

### ems-idempotency
| Key | Type | 설명 |
|-----|------|------|
| `message_id` (PK) | String | 발송 요청 ID |
| `ses_message_id` | String | SES 반환 메시지 ID |
| `ttl` | Number | TTL (24시간) |

## API Gateway 엔드포인트

| Method | 경로 | 인증 | 연결 | 설명 |
|--------|------|------|------|------|
| POST | `/send-email` | API Key | SQS | 이메일 발송 요청 |
| GET | `/results` | API Key | Lambda (event-query) | 발송결과 조회 |
| PUT | `/config` | API Key | Lambda (config-updater) → SSM | 수신 모드/콜백 설정 |
| POST | `/tenant-setup` | API Key | Lambda (tenant-setup) | 테넌트 생성, Identity/ConfigSet/템플릿 관리 |
| GET | `/tenant-status` | API Key | Lambda (tenant-setup) | 도메인 인증 상태 확인 |

## SSM Parameter Store

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| `/ems/mode` | `callback` | 발송결과 수신 모드 (callback / polling) |
| `/ems/callback_url` | (빈값) | ESM 콜백 수신 URL |
| `/ems/callback_secret` | (빈값) | 콜백 무결성 검증 시크릿 |

## 발송결과 수신 모드

| 모드 | Lambda 동작 | ESM 동작 | 사용 상황 |
|------|------------|---------|-----------|
| `callback` | DB저장 + ESM 콜백 호출 | 콜백 수신 + 보정 폴링 | 정상 운영 |
| `polling` | DB저장만 | 보정 폴링만 | ESM 장애, 콜백 포트 미개방 |

모드 전환: ESM UI → PUT /config → SSM → Lambda 30초 내 자동 반영

## 테넌트 온보딩 플로우

### 방식 A: 도메인 인증 (권장)
```
1. [UI] 인증 방식 선택: "도메인 인증" → 도메인 입력 → POST /tenant-setup
2. [Lambda] SES Identity 생성 → ConfigSet 생성 → SNS 연결 → DynamoDB 저장
3. [Lambda → UI] DKIM CNAME 레코드 3개 반환
4. [고객사] 자사 DNS에 CNAME 등록
5. [UI] GET /tenant-status 주기적 호출 → 인증 상태 확인
6. [Lambda] SES GetEmailIdentity → Verified → status: ACTIVE
→ 도메인 전체 발송 가능 (any@domain.com)
```

### 방식 B: 이메일 개별 인증 (DNS 접근 불가 시)
```
1. [UI] 인증 방식 선택: "이메일 인증" → 이메일 입력
2. [ESM] POST /onboarding/{tenantId}/verify-email → API Gateway → Lambda
3. [Lambda] SES CreateEmailIdentity(email) → SES가 인증 이메일 자동 발송
4. [사용자] 메일함에서 인증 링크 클릭
5. [UI] GET /onboarding/{tenantId}/email-status/{email} → 상태 확인
6. verificationStatus: SUCCESS → 해당 이메일로만 발송 가능
```

| 방식 | 인증 대상 | 인증 방법 | 발송 가능 범위 |
|------|-----------|-----------|----------------|
| 도메인 인증 | @domain.com 전체 | DNS CNAME 등록 | 도메인 내 모든 이메일 |
| 이메일 인증 | 개별 이메일 1개 | 인증 메일 링크 클릭 | 인증된 이메일만 |

## SQS 설정

| 항목 | 값 | 설명 |
|------|-----|------|
| visibilityTimeout | 180초 | Lambda 타임아웃(30초) × 6 |
| maxReceiveCount | 3 | DLQ 이동 전 최대 재시도 |
| reportBatchItemFailures | true | 부분 실패 시 실패 건만 재시도 |

## 보안

| 항목 | 방식 |
|------|------|
| ESM → API Gateway | x-api-key 헤더 인증 |
| API Gateway | IP Whitelist — CDK Context 또는 CfnParameter로 설정 |
| 전구간 | HTTPS (TLS) 암호화 |
| Lambda → ESM 콜백 | X-Callback-Secret 헤더 검증 |
| SSM | Callback Secret: 배포 후 `aws ssm put-parameter --type SecureString`으로 교체 필요 |
| CORS | 불필요 (서버 간 통신만 사용, 제거됨) |

## 비용 예상

| 환경 | 월 비용 |
|------|---------|
| 테스트 (Free Tier) | $0 |
| 테스트 (Free Tier 만료) | < $1 |
| 운영 (월 10만 통) | ~$13 |
