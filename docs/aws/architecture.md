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
│  │  DynamoDB: ems-tenant-config (TTL 1시간)             │    │
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
| `ttl` | Number | TTL (1시간) |

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

```
1. [UI] 도메인 입력 → POST /tenant-setup
2. [Lambda] SES Identity 생성 → ConfigSet 생성 → SNS 연결 → DynamoDB 저장
3. [Lambda → UI] DKIM CNAME 레코드 3개 반환
4. [고객사] 자사 DNS에 CNAME 등록
5. [UI] GET /tenant-status 주기적 호출 → 인증 상태 확인
6. [Lambda] SES GetEmailIdentity → Verified → status: ACTIVE
```

## 보안

| 항목 | 방식 |
|------|------|
| ESM → API Gateway | x-api-key 헤더 인증 |
| API Gateway | IP Whitelist (ESM 서버 IP만 허용) |
| 전구간 | HTTPS (TLS) 암호화 |
| Lambda → ESM 콜백 | X-Callback-Secret 헤더 검증 |
| SSM | Callback Secret은 SecureString 타입 |

## 비용 예상

| 환경 | 월 비용 |
|------|---------|
| 테스트 (Free Tier) | $0 |
| 테스트 (Free Tier 만료) | < $1 |
| 운영 (월 10만 통) | ~$13 |
