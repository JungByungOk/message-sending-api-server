# Backend Development Plan

> **기반 문서**: Joins EMS - Multi-tenant SaaS Architecture v7
> **목표**: 단일 테넌트 -> 멀티테넌트 SaaS 아키텍처 전환

---

## 현재 상태 (AS-IS)

- 단일 도메인 이메일 발송 (AWS SES 직접 호출)
- 단일 API Key 인증
- tenant 구분 없는 DB 구조
- DynamoDB 폴링 기반 이벤트 수집
- 단일 Quartz 스케줄러

## 목표 상태 (TO-BE)

- 고객사별 도메인 분리 발송 (멀티테넌트)
- 고객사별 API Key + tenant_id 인증
- AWS API Gateway -> SQS -> Lambda 비동기 파이프라인
- 고객사별 SES Config Set 자동 생성
- 셀프서비스 온보딩 포털
- tenant_id 기반 DB 격리

---

## Phase 1: 멀티테넌트 기반 구축

> DB 스키마 변경, Tenant 관리 API, 인증 체계 전환

### 1.1 Tenant Registry DB 설계 및 구축

**신규 테이블: `TENANT_REGISTRY`**

| Column | Type | Description |
|--------|------|-------------|
| tenant_id | VARCHAR(36) PK | 테넌트 고유 ID (UUID) |
| tenant_name | VARCHAR(100) | 고객사 이름 |
| domain | VARCHAR(255) | 발송 도메인 (예: a-corp.com) |
| api_key | VARCHAR(128) | 테넌트별 API Key |
| config_set_name | VARCHAR(100) | SES Configuration Set 이름 |
| verification_status | VARCHAR(20) | 도메인 인증 상태 (PENDING/VERIFIED/FAILED) |
| quota_daily | INT | 일일 발송 한도 |
| quota_monthly | INT | 월간 발송 한도 |
| status | VARCHAR(20) | 테넌트 상태 (ACTIVE/INACTIVE/SUSPENDED) |
| created_at | DATETIME | 생성일시 |
| updated_at | DATETIME | 수정일시 |

**기존 테이블 변경**

| Table | 변경 내용 |
|-------|----------|
| `ADM_EMAIL_SEND_MST` | `tenant_id` 컬럼 추가 |
| `ADM_EMAIL_SEND_DTL` | `tenant_id` 컬럼 추가 |

**작업 목록**
- [ ] Tenant Registry 테이블 DDL 작성
- [ ] 기존 테이블 ALTER 스크립트 작성 (tenant_id 추가)
- [ ] MyBatis mapper 업데이트 (tenant_id 조건 추가)
- [ ] 기존 데이터 마이그레이션 스크립트 (기본 tenant_id 부여)

### 1.2 Tenant Management API 개발

**패키지**: `com.msas.tenant`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/tenant` | 테넌트 등록 (도메인 입력, API Key 발급) |
| `GET` | `/tenant/{tenantId}` | 테넌트 정보 조회 |
| `GET` | `/tenants` | 테넌트 목록 조회 |
| `PATCH` | `/tenant/{tenantId}` | 테넌트 정보 수정 |
| `DELETE` | `/tenant/{tenantId}` | 테넌트 비활성화 |
| `GET` | `/tenant/{tenantId}/status` | 도메인 인증 상태 조회 |
| `POST` | `/tenant/{tenantId}/verify` | 도메인 인증 재시도 |

**작업 목록**
- [ ] TenantController 생성
- [ ] TenantService 생성
- [ ] TenantRepository (MyBatis mapper) 생성
- [ ] Tenant DTO 클래스 생성 (Request/Response)
- [ ] TenantEntity 생성
- [ ] Swagger 어노테이션 추가

### 1.3 인증 체계 전환 (멀티테넌트 API Key)

**현재**: 단일 API Key (`security.api-key`)
**변경**: 테넌트별 API Key + tenant_id 식별

**작업 목록**
- [ ] ApiKeyAuthenticationFilter 리팩토링
  - API Key로 Tenant Registry 조회
  - tenant_id를 SecurityContext에 저장
  - 요청별 tenant_id 자동 주입
- [ ] TenantContext (ThreadLocal) 유틸리티 생성
- [ ] 기존 API에 tenant_id 자동 바인딩 적용
- [ ] 하위 호환: 기존 단일 API Key도 지원 (기본 테넌트)

---

## Phase 2: SES 멀티테넌트 발송 전환

> 고객사별 SES Config Set, Domain Identity 자동화

### 2.1 SES Domain Identity 자동 등록

**AWS SES SDK v2 활용**

```
SES API 호출 흐름:
1. CreateEmailIdentity(domain) -> DKIM 레코드 3개 반환
2. 고객사에게 DNS 설정 안내 (CNAME 레코드)
3. GetEmailIdentity(domain) -> verification_status 폴링
4. Verified 시 tenant 상태 활성화
```

**작업 목록**
- [ ] SES Identity 관리 Service 생성 (`SESIdentityService`)
  - createDomainIdentity(domain) -> DKIM 레코드 반환
  - getDomainVerificationStatus(domain) -> 인증 상태 조회
  - deleteDomainIdentity(domain) -> 도메인 삭제
- [ ] DKIM 레코드 응답 DTO 생성
- [ ] 도메인 인증 상태 폴링 스케줄러 (ScheduledTask)

### 2.2 SES Configuration Set 자동 생성

**테넌트 등록 시 자동 생성 흐름**
```
Tenant 등록 -> Config Set 생성 (tenant-{tenant_id})
            -> Event Destination 설정 (SNS Topic)
            -> tenant_registry.config_set_name 업데이트
```

**작업 목록**
- [ ] SES Config Set 관리 Service 생성 (`SESConfigSetService`)
  - createConfigSet(tenantId) -> Config Set 생성
  - createEventDestination(configSetName, snsTopicArn) -> 이벤트 대상 설정
  - deleteConfigSet(configSetName) -> Config Set 삭제
- [ ] build.gradle에 AWS SES v2 추가 (sesv2 클라이언트)
- [ ] 테넌트 등록 시 Config Set 자동 생성 로직 연동

### 2.3 이메일 발송 멀티테넌트 적용

**변경 사항**
- 발송 시 tenant_id로 Config Set 이름 조회
- SES SendEmail에 ConfigurationSetName 파라미터 추가
- 메시지 태그에 tenant_id 포함

**작업 목록**
- [ ] SESMailService 리팩토링
  - sendEmail(): tenant_id -> config_set_name 조회 후 발송
  - sendTemplatedEmail(): 동일 적용
- [ ] Suppression List 테넌트별 격리
  - 테넌트별 Suppression 테이블 또는 tenant_id 컬럼 추가
- [ ] 발송 쿼터 관리 (일/월 한도 체크)

---

## Phase 3: AWS 비동기 파이프라인 연동

> API Gateway -> SQS -> Lambda 비동기 아키텍처 연동

### 3.1 API Gateway 연동 (미들웨어 -> AWS)

**현재**: 미들웨어 -> SES 직접 호출
**변경**: 미들웨어 -> API Gateway -> SQS -> Lambda -> SES

**작업 목록**
- [ ] AWS API Gateway 호출 클라이언트 생성 (`AwsApiGatewayClient`)
  - IAM Signature V4 서명 적용 (AWS SDK 자동 처리)
  - X-Tenant-ID 헤더 포함
- [ ] 발송 요청 흐름 전환
  - SES 직접 호출 -> API Gateway 호출로 변경
  - 비동기 응답 처리 (SQS 메시지 ID 반환)
- [ ] 설정 추가 (application.yml)
  - API Gateway endpoint URL
  - API Gateway stage

### 3.2 이벤트 수신 콜백 API 개발

**Lambda event-processor -> 미들웨어 콜백**

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ses/callback/delivery` | 발송 성공 이벤트 수신 |
| `POST` | `/ses/callback/bounce` | 반송 이벤트 수신 |
| `POST` | `/ses/callback/complaint` | 수신 거부 이벤트 수신 |

**작업 목록**
- [ ] SESCallbackController 생성
- [ ] 이벤트 수신 -> tenant_id별 상태 업데이트 로직
- [ ] 콜백 인증 (Lambda -> 미들웨어 보안)
- [ ] 기존 DynamoDB 폴링 방식과 병행 운영 (점진 전환)

### 3.3 Polling Checker 전환

**현재**: DynamoDB 직접 폴링
**변경**: Lambda 콜백 기반 (Phase 3.2 완료 후)

**작업 목록**
- [ ] PollingEmailFinalStatusFromDynamoDB -> 콜백 방식으로 점진 전환
- [ ] PollingNewEmailFromNFTDB에 tenant_id 필터 적용
- [ ] 폴링/콜백 병행 운영 설정 (feature flag)

---

## Phase 4: 온보딩 자동화 API

> 고객사 셀프서비스 온보딩 플로우

### 4.1 온보딩 플로우 API

```
온보딩 플로우:
1. 고객사 도메인 입력
2. SES Domain Identity 생성 -> DKIM 레코드 발급
3. 고객사 DNS 설정 안내
4. 인증 상태 폴링 (주기적 확인)
5. 인증 완료 -> API Key 활성화 -> Config Set 생성
```

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/onboarding/domain` | 도메인 등록 및 DKIM 레코드 발급 |
| `GET` | `/onboarding/{tenantId}/dkim` | DKIM 레코드 조회 |
| `GET` | `/onboarding/{tenantId}/verify` | 인증 상태 확인 |
| `POST` | `/onboarding/{tenantId}/activate` | 수동 활성화 (관리자용) |

**작업 목록**
- [ ] OnboardingController 생성
- [ ] OnboardingService 생성 (SES Identity + Config Set + Tenant 연동)
- [ ] 도메인 인증 상태 자동 폴링 스케줄러
- [ ] 인증 완료 시 자동 활성화 이벤트 처리
- [ ] Swagger 어노테이션 추가

### 4.2 발송 쿼터 관리

**작업 목록**
- [ ] 테넌트별 일/월 발송 쿼터 체크 Service
- [ ] SES Account 전체 한도 내 분배 로직
- [ ] 쿼터 초과 시 발송 거부 + 알림 처리
- [ ] 쿼터 사용량 조회 API

---

## Phase 5: 보안 강화

### 5.1 인터넷 구간 보안

**4중 보안 레이어 적용**

| Layer | 구현 |
|-------|------|
| Lambda 비노출 | SQS 트리거 방식 (외부 접근 불가) |
| IAM Signature V4 | AWS SDK 자동 서명 (미들웨어 -> API GW) |
| Security Group | Lambda 인바운드 차단, 아웃바운드 제한 |
| IP Whitelist | API Gateway Resource Policy (미들웨어 IP만 허용) |

**작업 목록**
- [ ] API Gateway IAM SigV4 호출 구현 (AWS SDK)
- [ ] tenant_id 도메인 검증 로직 (발신 도메인 == 등록 도메인)
- [ ] 발송 요청 시 tenant 상태 검증 (ACTIVE만 허용)

### 5.2 Suppression List 테넌트별 격리

**작업 목록**
- [ ] Suppression List 테이블에 tenant_id 추가
- [ ] 테넌트별 Suppression 조회/등록 API
- [ ] Bounce/Complaint 이벤트 -> 자동 Suppression 등록

---

## Phase 순서 및 의존성

```
Phase 1 (기반 구축)
  |-> 1.1 DB 스키마 (선행 필수)
  |-> 1.2 Tenant API (1.1 완료 후)
  +-> 1.3 인증 전환 (1.1, 1.2 완료 후)

Phase 2 (SES 멀티테넌트) -- Phase 1 완료 후
  |-> 2.1 Domain Identity
  |-> 2.2 Config Set
  +-> 2.3 발송 전환 (2.1, 2.2 완료 후)

Phase 3 (비동기 파이프라인) -- Phase 2 완료 후
  |-> 3.1 API Gateway 연동
  |-> 3.2 콜백 API
  +-> 3.3 Polling 전환 (3.2 완료 후)

Phase 4 (온보딩) -- Phase 2 완료 후 (Phase 3과 병행 가능)
  |-> 4.1 온보딩 API
  +-> 4.2 쿼터 관리

Phase 5 (보안) -- 전체 Phase 병행
  |-> 5.1 보안 레이어
  +-> 5.2 Suppression 격리
```

---

## 신규 패키지 구조

```
backend/src/main/java/com/msas/
|-- common/
|   |-- security/            # 멀티테넌트 인증 (UPDATED)
|   |-- swagger/             # Swagger 설정
|   |-- tenant/              # TenantContext (ThreadLocal) [NEW]
|   +-- ...
|-- tenant/                  # 테넌트 관리 모듈 [NEW]
|   |-- controller/
|   |-- service/
|   |-- repository/
|   |-- dto/
|   +-- entity/
|-- onboarding/              # 온보딩 모듈 [NEW]
|   |-- controller/
|   |-- service/
|   +-- dto/
|-- ses/                     # SES 모듈 (UPDATED)
|   |-- identity/            # Domain Identity 관리 [NEW]
|   +-- configset/           # Config Set 관리 [NEW]
|-- callback/                # SES 이벤트 콜백 모듈 [NEW]
|   |-- controller/
|   +-- service/
|-- gateway/                 # API Gateway 클라이언트 [NEW]
|-- scheduler/
+-- pollingchecker/          # (UPDATED - tenant_id 적용)
```

---

## 환경 설정 추가 (application.yml)

```yaml
# Multi-tenant
tenant:
  default-id: "default"       # 기존 단일 테넌트 호환
  quota:
    default-daily: 10000
    default-monthly: 300000

# AWS API Gateway
aws:
  api-gateway:
    endpoint: ${API_GATEWAY_ENDPOINT}
    stage: prod
    region: ap-northeast-2

# AWS SES v2 (추가)
  sesv2:
    region: ap-northeast-2
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}

# Onboarding
onboarding:
  verification-polling-interval: 300000  # 5분
```

---

## 비용 (최적안 기준)

| 서비스 | 월 비용 | 비고 |
|--------|---------|------|
| Amazon SES | $10.00 | 100K건 x $0.10/1K |
| API Gateway | $0.10 | 100K x $1.00/1M |
| SQS | $0.00 | 무료 티어 |
| Lambda | $0.00 | 무료 티어 |
| CloudWatch | $0.00 | 무료 티어 |
| **합계** | **$10.10/월** | **연간 ~$121** |

> Valkey(캐시), NAT Gateway 미사용으로 비용 최적화
> 월 50만건 초과 시 Valkey 도입 재검토
