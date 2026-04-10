# Joins EMS - AWS SES 네이티브 마이그레이션 최종 설계

**작성일:** 2026-04-09
**최종 수정:** 2026-04-10
**상태:** 최종 설계

---

## 1. 프로젝트 개요

AWS SES 기반 대용량 이메일 발송 솔루션(Joins EMS)을 SES 네이티브 기능을 최대한 활용하는 구조로 전환한다.

**핵심 목표:**
- SES Multi-Tenant으로 테넌트별 평판 격리 및 자동 관리
- Callback 제거, 이벤트 파이프라인을 EventBridge 기반으로 통합
- SQS + Lambda 병렬 발송으로 대용량 처리 고도화
- 자체 구현 최소화, AWS 관리형 서비스 최대 활용

---

## 2. 확정된 설계 결정

| 결정 | 내용 |
|------|------|
| SES Multi-Tenant | ap-northeast-2 지원 확인, 도입 확정 |
| Callback 제거 | 폴링 전용 → EventBridge 전환 |
| SNS → EventBridge | 이벤트 라우팅 통합, 테넌트 상태 변화 수신 |
| 대용량 발송 | Quartz + API Gateway → SQS + Lambda 병렬 발송 |
| IDC DB | PostgreSQL 유지, AWS에서 HTTP API로만 접근 |

---

## 3. To-Be 아키텍처

### 3.1 전체 구조도

```
┌──────────────────────────────────────────────────────────────────┐
│  AWS Cloud (ap-northeast-2)                                      │
│                                                                  │
│  ┌─── SES (Multi-Tenant) ─────────────────────────────────┐     │
│  │  Tenant A ── Identity A + ConfigSet A + 평판정책        │     │
│  │  Tenant B ── Identity B + ConfigSet B + 평판정책        │     │
│  │  (전송 일시정지/재개, 메트릭 자동 분리, 평판 조사 결과)     │     │
│  └──────┬──────────────────────────────────▲───────────────┘     │
│         │ 이벤트 발생                       │ 발송                │
│         ▼                                  │                     │
│  ┌─── EventBridge ──────────────────┐      │                     │
│  │                                  │      │                     │
│  │  Rule 1: Bounce/Complaint        │      │                     │
│  │    → Lambda (suppression)        │      │                     │
│  │                                  │      │                     │
│  │  Rule 2: Delivery/Open/Click/..  │      │                     │
│  │    → Lambda (event-processor)    │      │                     │
│  │                                  │      │                     │
│  │  Rule 3: 테넌트 상태 변화          │      │                     │
│  │    → Lambda (tenant-sync)        │      │                     │
│  └──────┬───────────┬───────────┬───┘      │                     │
│         ▼           ▼           ▼          │                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐   │                     │
│  │ Lambda   │ │ Lambda   │ │ Lambda   │   │                     │
│  │ suppress.│ │ event-   │ │ tenant-  │   │                     │
│  │          │ │ processor│ │ sync     │   │                     │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘   │                     │
│       │            │            │          │                     │
│       ▼            ▼            ▼          │                     │
│  ┌─── DynamoDB ──────────────────────┐     │                     │
│  │  ems-send-results (이벤트, TTL 7d) │     │                     │
│  │  ems-tenant-config (테넌트 설정)    │     │                     │
│  │  ems-suppression (수신거부 동기화)   │     │                     │
│  └──────────────▲────────────────────┘     │                     │
│                 │                           │                     │
│  ┌─── SQS ─────┼───────────────────────┐   │                     │
│  │              │                       │   │                     │
│  │  email-send-queue ──→ Lambda ────────┼───┘                    │
│  │  (배치 크기 10, 동시성 제어)  (email-sender)                     │
│  │              │         + TenantName                            │
│  │  email-send-dlq (실패 건)                                      │
│  │              │                                                 │
│  └──────────────┼─────────────────────────────┘                  │
│                 │                                                 │
│  ┌─── API Gateway ───────────────────────┐                       │
│  │  /tenant-setup  (테넌트 관리)           │                       │
│  │  /event-query   (이벤트 조회)           │                       │
│  │  /email-enqueue (발송 큐 등록)          │                       │
│  └───────────────────────────────────────┘                       │
│                 │                                                 │
│  ┌─── CloudWatch ─────┐  ┌─── Cost Explorer ──┐                 │
│  │  SES 테넌트별 메트릭  │  │  실 비용 조회        │                 │
│  └─────────────────────┘  └────────────────────┘                 │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
                              ▲
                              │ HTTP API (폴링/호출)
                              │
┌─────────────────────────────┼────────────────────────────────────┐
│  IDC / Backend (Spring Boot)│                                    │
│                             │                                    │
│  ┌── TenantSetupService ────┘                                   │
│  │  테넌트 CRUD → API GW → Lambda → SES CreateTenant            │
│  │  일시정지/재개, 평판 정책 설정                                    │
│  └──────────────────────────────────────────────                 │
│                                                                  │
│  ┌── EmailDispatchService (신규) ──────────────────┐             │
│  │  발송 요청 → API GW → Lambda(enqueue) → SQS     │             │
│  │  소량(≤1,000): body에 수신자 포함                 │             │
│  │  대량(>1,000): S3 업로드 + S3 key 전달            │             │
│  │  배치 진행률: SQS 메시지 수 + DynamoDB 상태 조회     │             │
│  └─────────────────────────────────────────────────┘             │
│                                                                  │
│  ┌── ResultPollingService ─────────────────────────┐             │
│  │  DynamoDB 폴링 (1~2분 주기) → 로컬 DB 업데이트      │             │
│  │  이벤트 리플레이 필요 시 EventBridge Archive 활용    │             │
│  └─────────────────────────────────────────────────┘             │
│                                                                  │
│  ┌── MonitoringService ────────────────────────────┐             │
│  │  CloudWatch 테넌트별 메트릭 조회 (5분 캐시)         │             │
│  │  Cost Explorer 실 비용 조회 (1일 캐시)             │             │
│  │  로컬 DB 통계와 병합                               │             │
│  └─────────────────────────────────────────────────┘             │
│                                                                  │
│  ┌── Quartz (축소) ────────────────────────────────┐             │
│  │  SendingTimeoutChecker (10분 주기)               │             │
│  │  ResultPollingService 트리거 (1~2분 주기)         │             │
│  │  Suppression 동기화 (1일 1회)                     │             │
│  └─────────────────────────────────────────────────┘             │
│                                                                  │
│  ┌── PostgreSQL ──────────────────────────────────────┐             │
│  │  테넌트 (+ ses_tenant_name)                      │             │
│  │  발송 이력 (장기 보관)                              │             │
│  │  Suppression (테넌트별 격리)                       │             │
│  │  할당량 (일별/월별)                                 │             │
│  └─────────────────────────────────────────────────┘             │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 As-Is → To-Be 변경 요약

| 항목 | As-Is | To-Be |
|------|-------|-------|
| **이벤트 수신** | SNS → Lambda → DynamoDB + Callback | **EventBridge → Lambda(역할별 분리) → DynamoDB** |
| **이벤트 라우팅** | 단일 Lambda에서 전체 분기 | **EventBridge Rule로 이벤트 타입별 라우팅** |
| **테넌트 관리** | 로컬 DB만 | **로컬 DB + SES Tenant 동기화** |
| **평판 관리** | 미구현 | **SES 네이티브 (정책, 자동 정지, 조사 결과)** |
| **전송 제어** | 미구현 | **SES 일시정지/재개** |
| **대량 발송** | Quartz → API GW → Lambda (순차, 14/s) | **SQS → Lambda 병렬 (동시성 제어, 자동 확장)** |
| **속도 제어** | Guava RateLimiter 고정 | **SES GetAccount → SQS 동시성 동적 조정** |
| **실패 재처리** | 수동 | **SQS DLQ 자동 재처리** |
| **모니터링** | 로컬 DB 자체 쿼리 | **CloudWatch 테넌트 메트릭 + 로컬 DB 병합** |
| **비용 추정** | 하드코딩 단가 | **Cost Explorer 실 비용** |
| **Callback** | 6개 파일 + Lambda 로직 | **완전 제거** |
| **Quartz 역할** | 배치 발송 + 폴링 + 타임아웃 | **폴링 + 타임아웃 + 동기화만 (배치 발송은 SQS로 이관)** |

### 3.3 테넌트 기준 전체 라이프사이클

이메일 발송부터 모니터링까지 모든 흐름이 **테넌트 단위로 격리**되어 관리된다.

```
┌─────────────────────────────────────────────────────────────┐
│  테넌트 A 라이프사이클                                        │
│                                                             │
│  [1. 발송 요청]                                              │
│    ├── 테넌트 A 할당량 검증 (일별/월별)                        │
│    ├── 테넌트 A Suppression 필터링                            │
│    └── API GW → Lambda(enqueue) → SQS 등록                  │
│                                                             │
│  [2. SES 발송]                                               │
│    └── SendEmail({ TenantName: "A", ConfigSetName: "A" })   │
│        → SES가 테넌트 A 메트릭으로 자동 집계                    │
│                                                             │
│  [3. 이벤트 수신] EventBridge                                 │
│    ├── Delivery  → DynamoDB (tenant_id: A)                  │
│    ├── Bounce    → DynamoDB + 테넌트 A Suppression 등록      │
│    ├── Complaint → DynamoDB + 테넌트 A Suppression 등록      │
│    └── Open/Click → DynamoDB (tenant_id: A)                 │
│                                                             │
│  [4. 폴링 → 로컬 DB 동기화]                                   │
│    ├── 테넌트 A 발송 이력 업데이트                              │
│    └── 테넌트 A 사용량 카운트 갱신                              │
│                                                             │
│  [5. 모니터링] 모두 테넌트 A 기준                               │
│    ├── CloudWatch: 테넌트 A 전달률/반송율/불만율                │
│    ├── SES Console: 테넌트 A 평판 상태/조사 결과               │
│    ├── 대시보드: 테넌트 A 할당량 사용률                         │
│    └── 비용: 테넌트 A 발송량 기반 비용 추정                     │
│                                                             │
│  [6. 평판 관리] SES 네이티브                                   │
│    ├── 평판 정책 (Standard/Strict/None)                      │
│    ├── 반송율 초과 시 자동 일시정지                             │
│    └── 관리자 수동 일시정지/재개                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**테넌트 격리 보장 포인트:**

| 단계 | 격리 방법 | 격리 수준 |
|------|----------|----------|
| 발송 | SES `TenantName` 파라미터 | SES 네이티브 |
| 할당량 | QuotaService 테넌트별 일/월 한도 | Backend |
| Suppression | 로컬 DB `tenant_id` 기반 필터링 | Backend |
| 이벤트 | DynamoDB `tenant_id` 파티션 키 | AWS |
| 메트릭 | CloudWatch 테넌트 차원(dimension) | SES 네이티브 |
| 평판 | SES 테넌트별 평판 정책 | SES 네이티브 |
| 발송 이력 | PostgreSQL `tenant_id` 기반 조회 | Backend |

---

### 3.4 발송 요청 흐름 (API Gateway 경유)

Backend는 항상 **API Gateway 1회 호출**로 발송을 요청한다. Lambda(enqueue)가 SQS 등록을 처리.

#### 즉시 발송

```
Backend (EmailDispatchService)
  │
  ├── 1. 테넌트 할당량 검증 (QuotaService)
  ├── 2. 발송 마스터 정보 PostgreSQL 저장
  ├── 3. API Gateway 호출 (1회)
  │
  ▼
POST /email-enqueue (API Gateway → Lambda enqueue)
  │
  ├── [소량 ≤ 1,000건] body에 수신자 포함
  │   {
  │     "tenantId": "tenant-a",
  │     "templateName": "welcome-email",
  │     "recipients": [
  │       { "to": "user1@example.com", "templateData": { "userName": "홍길동" } },
  │       { "to": "user2@example.com", "templateData": { "userName": "김철수" } }
  │     ]
  │   }
  │
  ├── [대량 > 1,000건] S3 참조
  │   {
  │     "tenantId": "tenant-a",
  │     "templateName": "welcome-email",
  │     "s3Key": "batch/12345.json"
  │   }
  │   (Backend가 수신자 목록을 S3에 미리 업로드)
  │
  ▼
Lambda (enqueue)
  ├── body에서 수신자 추출 또는 S3에서 읽기
  ├── 테넌트 설정 조회 (DynamoDB: sesTenantName, configSetName)
  ├── 수신자별 SQS 메시지 생성
  └── SQS SendMessageBatch (10건씩 배치 등록)
        │
        ▼
  SQS (email-send-queue) → Lambda (email-sender) → SES
```

**SQS 메시지 구조 (수신자 1건 = 메시지 1건):**

```json
{
  "emailSendSeq": 12345,
  "emailSendDtlSeq": 67890,
  "tenantId": "tenant-a",
  "sesTenantName": "ses-tenant-a",
  "configSetName": "tenant-a-config",
  "correlationId": "corr-uuid-001",
  "from": "no-reply@tenant-a.com",
  "to": "user@example.com",
  "templateName": "welcome-email",
  "templateData": {
    "userName": "홍길동",
    "verifyLink": "https://example.com/verify/abc123"
  }
}
```

메시지 크기: 약 500B~2KB (256KB 제한에 여유 충분)
Lambda(email-sender)는 **DB 접근 없이** 메시지 내 정보만으로 독립 발송.

**API Gateway 제한 및 대응:**

| 제한 | 값 | 대응 |
|------|---|------|
| Payload 크기 | 최대 10MB | 대량은 S3 참조 방식 |
| 타임아웃 | 최대 29초 | Lambda 내부에서 SQS 배치 등록 (AWS 내부 통신, 빠름) |

#### 예약 발송

```
[예약 발송 요청] "2026-04-15 09:00에 10,000건 발송"
  │
  ├── 1. 테넌트 할당량 사전 검증
  ├── 2. 발송 마스터 PostgreSQL 저장 (예약 상태)
  ├── 3. 대량이면 수신자 목록 S3 업로드
  ├── 4. EventBridge Scheduler 등록 (일회성)
  │       → 지정 시간에 Lambda(enqueue) 호출
  │
  ▼ (2026-04-15 09:00 도달)
  │
  EventBridge Scheduler → Lambda (enqueue)
  │
  ├── S3/body에서 수신자 읽기
  ├── SQS 메시지 배치 등록
  │
  ▼
  SQS → Lambda (email-sender) → SES
```

| 방식 | 즉시 발송 | 예약 발송 |
|------|----------|----------|
| 트리거 | Backend → API GW → Lambda | EventBridge Scheduler → Lambda |
| 일시정지/재개 | SQS 메시지 소비 중지 | Scheduler 일시정지/재개 |
| 취소 | SQS 메시지 삭제 | Scheduler 삭제 |

---

### 3.5 발송 처리 상세 (SQS → Lambda → SES)

```
SQS (email-send-queue)
  ├── BatchSize: 10
  ├── MaximumConcurrency: SES maxSendRate 기반 동적 설정
  └── DLQ: email-send-dlq (3회 실패 시)
        │
        ▼
Lambda (email-sender)
  ├── SQS 메시지에서 발송 정보 추출 (DB 접근 불필요)
  ├── SES SendEmail({ TenantName, ConfigSetName })
  ├── 성공 → DynamoDB 상태 업데이트 (Sending)
  └── 실패 → SQS 자동 재시도 또는 DLQ
        │
        ▼
SES → EventBridge → Lambda(event-processor) → DynamoDB
        │
        ▼
Backend (ResultPollingService) → 로컬 DB 업데이트
```

**처리량 예시:**

| SES 쿼터 | Lambda 동시성 | 예상 처리량 |
|---------|-------------|-----------|
| 14/s (Sandbox) | 2 | ~14건/초 |
| 50/s | 5 | ~50건/초 |
| 200/s | 20 | ~200건/초 |
| 500/s | 50 | ~500건/초 |

---

### 3.6 EventBridge 이벤트 라우팅 상세

| Rule | 이벤트 패턴 | 타겟 | 처리 내용 |
|------|-----------|------|----------|
| Rule 1 | `eventType: [Bounce, Complaint]` | Lambda (suppression) | DynamoDB 저장 + Suppression 등록 |
| Rule 2 | `eventType: [Send, Delivery, Open, Click, Reject, DeliveryDelay, RenderingFailure]` | Lambda (event-processor) | DynamoDB 저장 |
| Rule 3 | `source: ses.amazonaws.com, detail-type: Tenant Status Change` | Lambda (tenant-sync) | DynamoDB 테넌트 상태 동기화 |
| Archive | 전체 이벤트 | EventBridge Archive | 30일 보관, 장애 시 리플레이 |

---

## 4. 할당량 관리 (2계층 구조)

### 4.1 SES 네이티브 한도 vs 자체 구현 한도

**SES 테넌트에는 발송 한도 설정 기능이 없다.** SES 테넌트는 **평판 격리** 목적이며, 발송 쿼터는 계정(Account) 수준에서만 존재한다. AWS도 테넌트별 발송 한도는 애플리케이션 레벨 구현을 권장한다.

| 구분 | SES 네이티브 | 자체 구현 (Backend) |
|------|:-:|:-:|
| 계정 일일 발송 한도 | O (`GetAccount`) | - |
| 계정 초당 전송률 | O (`MaxSendRate`) | - |
| 테넌트별 일일 한도 | **X** | **O** (`QuotaService`) |
| 테넌트별 월별 한도 | **X** | **O** (`QuotaService`) |
| 테넌트별 TPS 제한 | **X** | **O** (SQS 동시성 제어) |
| 테넌트별 평판 격리 | O (자동) | - |
| 테넌트별 자동 일시정지 | O (평판 기반) | - |

**역할 분리:**
- **SES 테넌트** → 평판 격리, 자동 일시정지, 메트릭 분리
- **Backend QuotaService** → 발송 한도, 사용량 카운트, 초과 차단

### 4.2 구조

```
AWS 계정 한도 (SES GetAccount) ─── 관리자 대시보드 표시
  ├── 일일 발송 한도 (Max24HourSend)
  ├── 초당 전송률 (MaxSendRate)
  └── 24시간 사용량 (SentLast24Hours)
      │
      ├── Tenant A: 일별/월별 할당량 ─── 테넌트별 사용률 표시 (Backend 관리)
      ├── Tenant B: 일별/월별 할당량
      └── Tenant C: 일별/월별 할당량
```

### SES Sending Quota 단계

| 단계 | 일 발송 한도 | 초당 전송률 (TPS) | 조건 |
|------|------------|-----------------|------|
| Sandbox | 200건/일 | 1 TPS | 가입 직후 |
| Production 초기 | 50,000건/일 | 14 TPS | Sandbox 해제 후 기본 |
| 자동 증가 | 최대 ~100만 건/일 | 자동 조정 | reputation 양호 + 한도 근처 사용 시 |
| 수동 요청 | 수백만 건 이상 | 수백~수천 TPS | Service Quotas에서 증설 요청 |

### 증설 방법

**1) 자동 증가 (~100만 건/일까지)**

일일 발송량이 현재 한도에 가까워지면, bounce/complaint rate가 낮은 계정에 대해 AWS가 자동으로 quota를 올려준다. 별도 요청 없이 자동으로 증가하며, 약 100만 건/일까지 가능.

**2) 수동 요청 (그 이상)**

Service Quotas 콘솔에서 Sending quota(일 발송량)와 Sending rate(TPS) 증설을 요청.

AWS가 확인하는 항목:
- bounce rate 5% 미만, complaint rate 0.1% 미만 유지
- 정당한 발송 사유 설명 (마케팅, 트랜잭션 등)
- 수신자 확보 방법 (opt-in 방식)
- 기존 이메일 제공자에서의 과거 발송 실적 데이터 (있으면 유리)

보통 24시간 이내 응답. 요청이 잘 정리되어 있으면 더 빠름.

### 증설 시 주의사항 - Ramp-up

급격한 볼륨 증가는 피해야 한다. **주당 20~30%씩 점진적으로** 볼륨을 올려야 sender reputation이 안정적으로 쌓인다. 하루 5만 건에서 바로 50만 건으로 점프하면 ISP들이 의심하고 차단할 수 있다.

**현실적인 ramp-up 계획 예시:**

```
1주차:  50,000건/일
2주차:  65,000건/일 (+30%)
3주차:  85,000건/일
4주차: 110,000건/일
5주차: 145,000건/일
6주차: 190,000건/일
...
```

**참고:**
- Quota는 **AWS 리전별로 별도** 관리. 서울 리전 증설이 도쿄 리전에 적용되지 않음.
- 하루 5만 건 규모라면 Production 초기 기본값으로 바로 사용 가능.
- 수백만 건/일까지도 증설 가능하므로 SES의 스케일 한계를 걱정할 필요 없음.

### 4.3 관리자 기능

| 기능 | 데이터 소스 | 표시 위치 |
|------|-----------|----------|
| AWS 계정 전체 한도/사용량 | SES `GetAccount` API (실시간) | 관리자 대시보드 |
| 계정 잔여 한도 | Max24HourSend - SentLast24Hours | 관리자 대시보드 |
| 테넌트 전체 할당량 합계 vs 계정 한도 | 로컬 DB 집계 vs SES | 관리자 대시보드 (초과 할당 경고) |

### 4.4 테넌트별 기능

| 기능 | 데이터 소스 | 표시 위치 |
|------|-----------|----------|
| 일별 할당량 / 사용량 / 사용률 | 로컬 DB (QuotaService) | 테넌트 상세, 대시보드 |
| 월별 할당량 / 사용량 / 사용률 | 로컬 DB (QuotaService) | 테넌트 상세, 대시보드 |
| 한도 초과 시 발송 차단 | QuotaService 실시간 검증 | 발송 API 응답 |

### 4.5 발송 시 검증 흐름

```
발송 요청
  │
  ├── 1. AWS 계정 한도 확인 (SES GetAccount)
  │     └── 계정 잔여 한도 < 요청 건수 → 거부 (HTTP 429)
  │
  ├── 2. 테넌트 할당량 확인 (QuotaService)
  │     ├── 일별 한도 초과 → 거부 (HTTP 429)
  │     └── 월별 한도 초과 → 거부 (HTTP 429)
  │
  ├── 3. Suppression 필터링
  │
  └── 4. SQS 등록 → Lambda → SES 발송
```

### 4.6 SQS 동시성 동적 제어

```
SES GetAccount → maxSendRate (예: 50/s)
  │
  └── SQS Lambda 동시성 = maxSendRate / BatchSize
      예: 50/s ÷ 10 = 동시성 5
```

주기적으로 (5분) SES 쿼터를 조회하여 SQS Lambda 동시성을 자동 조정. 쿼터 변경(Sandbox → Production) 시 재배포 없이 자동 반영.

---

## 5. 인증/인가 구조 (JWT 우선)

### 5.1 이중 인증 구조

```
관리자 (대시보드):  JWT → 모든 API 접근 가능 (테넌트 지정 발송 포함)
외부 테넌트:       API Key → 발송 API만 접근 (자기 테넌트만)
```

```
요청 수신
  │
  ├── Authorization: Bearer eyJhbG... (JWT 형식)
  │     └── JWT 검증 → 관리자 권한 → 모든 API 접근
  │
  ├── Authorization: Bearer sk-xxxx... (API Key 형식)
  │     └── API Key 조회 → 테넌트 식별 → 발송 API만 접근
  │
  └── 인증 헤더 없음 → 401 Unauthorized
```

### 5.2 API 접근 권한

| API | JWT (관리자) | API Key (테넌트) |
|-----|:----------:|:--------------:|
| `/auth/*` | O | X |
| `/users/*` | O | X |
| `/tenant/*` | O | X |
| `/tenant-setup/*` | O | X |
| `/monitoring/*` | O | X |
| `/settings/*` | O | X |
| `/suppression/*` | O | X |
| `/scheduler/*` | O | X |
| `/email-enqueue` | O (테넌트 지정) | O (자기 테넌트만) |
| `/ses/templates` | O | O (자기 테넌트만) |

### 5.3 JWT 토큰

- accessToken: 30분 만료
- refreshToken: 7일 만료
- 미사용 30분 → 자동 로그아웃 (Frontend 타이머)

### 5.4 사용자 관리

- 초기 계정: admin / admin (PostgreSQL `ADM_USER_MST` 테이블)
- 추후 사용자 추가 기능 지원
- 비밀번호 변경 기능

### 5.5 Auth API

| API | Method | 설명 |
|-----|--------|------|
| `/auth/login` | POST | 로그인 → JWT 발급 |
| `/auth/refresh` | POST | 토큰 갱신 |
| `/auth/logout` | POST | 로그아웃 (refreshToken 무효화) |
| `/auth/change-password` | PUT | 비밀번호 변경 |
| `/users` | GET | 사용자 목록 |
| `/users` | POST | 사용자 추가 |
| `/users/{id}` | PUT | 사용자 수정 |
| `/users/{id}` | DELETE | 사용자 삭제 |
| `/users/me` | GET | 내 정보 조회 |

### 5.6 구현 Phase

로그인/인증 기능은 **Phase 1 (코드 품질 정비)에 포함**하여 이후 Phase의 API 보안 기반을 확보한다.

---

## 6. 자체 구현 유지 영역 (SES 대체 불가)

| 영역 | 이유 | 위치 |
|------|------|------|
| JWT 인증 (관리자) | SES에 대시보드 인증 없음 | `AuthController`, `JwtFilter` |
| API Key 인증 (테넌트) | SES에 외부 사용자 인증 없음 | `ApiKeyAuthenticationFilter` |
| 테넌트별 일/월 할당량 | SES는 계정 수준만 | `QuotaService` |
| 발송 이력 장기 보관 | SES 이벤트는 일시적 | PostgreSQL |
| correlationId 추적 | SES MessageId만으로 불가 | `ResultPollingService` |
| 테넌트별 Suppression 격리 | SES는 계정 수준 | PostgreSQL `ADM_EMAIL_BL_MST` |
| 템플릿-테넌트 매핑 | SES에 접근 제어 없음 | `TemplateTenantRepository` |

---

## 7. Quartz 변화

배치 발송/예약 발송/속도 제어는 AWS로 이관하고, 주기 작업 3개만 Quartz에 유지한다.

| 현재 (As-Is) | 전환 후 (To-Be) | 상태 |
|-------------|----------------|------|
| 배치 발송 Job | SQS + Lambda로 이관 | **제거** |
| 예약 발송 Job | EventBridge Scheduler로 이관 | **제거** |
| RateLimiter | SQS 동시성 + SES 쿼터 동적 조회 | **제거** |
| ResultPollingService 트리거 (1~2분) | 유지 | **유지** |
| SendingTimeoutChecker (10분) | 유지 | **유지** |
| Suppression 동기화 (1일 1회) | 유지 | **유지** |

Quartz에 남는 작업은 모두 Backend(IDC)에서 PostgreSQL에 직접 접근하는 주기 작업이므로, VPN 없이 현재 구조 그대로 동작한다.

---

## 8. 제거 대상 코드

### 8.1 Callback 관련 (Phase 2)

| 파일 | 역할 |
|------|------|
| `SESCallbackService.java` | Callback 이벤트 처리 |
| `SESCallbackController.java` | Callback 엔드포인트 |
| `SESCallbackEventDTO.java` | Callback DTO |
| `CallbackResponseDTO.java` | Callback 응답 DTO |
| `CallbackSecretFilter.java` | Callback 인증 필터 |
| `SESFeedbackNotificationController.java` | 레거시 피드백 (dead code) |
| event-processor Lambda Callback 로직 | SSM 조회 + HTTP 호출 |
| SSM `/ems/mode`, `/ems/callback_url`, `/ems/callback_secret` | Callback 설정 |

### 8.2 대용량 발송 전환 시 (Phase 3)

| 파일 | 역할 | 대체 |
|------|------|------|
| `AbstractSendTemplatedEmailJob.java` | 배치 발송 기반 클래스 | SQS + Lambda |
| `SendTemplatedEmailJob.java` | 예약/배치 발송 Job | SQS + EventBridge Scheduler |
| `SendTemplatedEmailWithPollingJob.java` | 폴링 배치 발송 Job | SQS |
| `SesRateLimiterConfig.java` | Guava RateLimiter | SQS 동시성 + SES 쿼터 동적 조회 |

---

## 9. 마이그레이션 전략

### 9.1 레이어별 작업 순서

각 Phase 내에서 **AWS(CDK) → Backend → Frontend** 순서로 진행한다. 인프라가 먼저 준비되어야 Backend가 연동하고, Backend API가 준비되어야 Frontend가 호출할 수 있다.

```
AWS (CDK)          Backend              Frontend
  │                  │                    │
  ├── EventBridge    │                    │
  ├── SQS 큐        │                    │
  ├── Lambda 분리    │                    │
  ├── SES Tenant     │                    │
  │   IAM 정책       │                    │
  │                  │                    │
  │ ✅ 인프라 준비    ├── Callback 제거    │
  │                  ├── 신규 Service     │
  │                  ├── API 추가/변경    │
  │                  │                    │
  │                  │ ✅ API 준비        ├── 대시보드 개편
  │                  │                    ├── 테넌트 관리 UI
  │                  │                    └── 모니터링 UI
```

### 9.2 CDK 전략: 기존 삭제 후 신규 작성

**기존 CDK 스택을 삭제하고 v2 CDK를 신규 작성한다.**

현재 테넌트 0건, 운영 데이터 없으므로 삭제 리스크 없음.

**삭제 대상 (기존):**

| 리소스 | 비고 |
|--------|------|
| Lambda (email-sender, event-processor, tenant-setup, config-updater, event-query) | v2에서 신규 생성 |
| DynamoDB (ems-send-results, ems-tenant-config) | v2에서 신규 생성 |
| API Gateway | v2에서 신규 생성 |
| SNS Topic | v2에서 EventBridge로 대체 |
| IAM Role | v2에서 신규 생성 |

**v2 CDK 신규 구조:**

```
aws/ems-cdk-v2/
  ├── lib/
  │   └── ems-v2-stack.ts
  │       ├── EventBridge (Rule 3개 + Archive)
  │       ├── SQS (email-send-queue + DLQ)
  │       ├── DynamoDB (ems-send-results, ems-tenant-config, ems-suppression)
  │       ├── API Gateway (/tenant-setup, /event-query, /email-enqueue)
  │       ├── Lambda
  │       │   ├── email-sender (SQS 트리거, TenantName)
  │       │   ├── enqueue (API GW 트리거)
  │       │   ├── event-processor (EventBridge 트리거)
  │       │   ├── suppression (EventBridge 트리거)
  │       │   ├── tenant-setup (API GW 트리거, SES Tenant CRUD)
  │       │   ├── tenant-sync (EventBridge 트리거)
  │       │   └── event-query (API GW 트리거)
  │       ├── S3 (대량 발송 수신자 목록)
  │       └── IAM (SES Tenant API, CloudWatch, Cost Explorer 권한)
  │
  └── lambda/
      ├── email-sender/      (신규)
      ├── enqueue/           (신규)
      ├── event-processor/   (신규)
      ├── suppression/       (신규)
      ├── tenant-setup/      (신규)
      ├── tenant-sync/       (신규)
      └── event-query/       (신규)
```

### 9.3 배포 전략

| 방식 | 설명 | 적용 |
|------|------|------|
| **기존 CDK 스택 삭제** | 기존 리소스 전체 삭제 후 v2 배포 | Phase 2 시작 시 |
| **v2 CDK 배포** | 신규 스택으로 전체 리소스 생성 | Phase 2 |
| **Backend Feature Flag** | `ses.tenant.enabled=false` → 테스트 후 `true` 전환 | Phase 2, 3 |
| **병행 운영** | 기존 SNS + 신규 EventBridge 동시 운영 기간 | Phase 2 전환기 |
| **Frontend 점진 배포** | Backend API 준비 후 UI 배포 | 매 Phase Step 3 |

### 9.4 롤백 시나리오

```
문제 발생 시:
  AWS:     CDK rollback (이전 스택 복원)
  Backend: Feature Flag off + 이전 버전 배포
  Frontend: 이전 빌드 재배포
```

### 9.5 브랜치 전략

```
main (현재 코드 유지)
  └── feature/ses-native-migration
        ├── phase1/code-quality          (Backend만)
        ├── phase2/eventbridge-tenant    (CDK → Backend → Frontend)
        ├── phase3/sqs-pipeline          (CDK → Backend → Frontend)
        └── phase4/monitoring            (CDK → Backend → Frontend)
```

각 Phase를 sub-branch로 작업 후 `feature/ses-native-migration`에 머지, 최종적으로 `main`에 머지.

---

## 10. Phase별 실행 계획

### Phase 1: 코드 품질 기반 정비 (3~5일)

**목표:** 이후 Phase의 안전한 진행을 위한 코드 구조 개선

| TODO | 내용 | 대상 |
|------|------|------|
| 1.1 | MonitoringController 393줄 → 서비스 분리 (100줄 이하) | `MonitoringService`, `CostEstimateService` 신규 |
| 1.2 | `Map<String, Object>` 응답 → 전용 DTO | 모든 Controller |
| 1.3 | `Map<String, String>` 수신 → DTO + `@Valid` | 요청 Validation |

**수용 기준:**
- Controller에 `Map<String, Object>` 반환 없음
- Swagger UI에서 모든 응답 스키마 표시
- 기존 프론트엔드 호환성 유지

---

### Phase 2: Callback 제거 + EventBridge 전환 + SES Multi-Tenant (7~10일)

**목표:** 이벤트 파이프라인을 EventBridge 기반으로 통합, SES 네이티브 테넌트 연동

| TODO | 내용 |
|------|------|
| 2.1 | Callback 관련 코드 전면 제거 (6개 파일 + Lambda 로직) |
| 2.2 | SNS → EventBridge 전환 (CDK 스택 변경) |
| 2.3 | EventBridge Rule 설정 (이벤트 타입별 라우팅, Archive 30일) |
| 2.4 | Lambda 분리: event-processor, suppression, tenant-sync |
| 2.5 | Lambda tenant-setup에 SES Tenant CRUD 추가 (`CreateTenant`, `GetTenant` 등) |
| 2.6 | Backend 테넌트 흐름에 SES Tenant 동기화 (`ses_tenant_name` 컬럼) |
| 2.7 | email-sender Lambda에 `TenantName` 주입 |
| 2.8 | 테넌트 일시정지/재개 API + 평판 정책 설정 |
| 2.9 | Suppression 처리를 ResultPollingService로 이관 |
| 2.10 | 폴링 주기 최적화 (5분 → 1~2분) |

**SES API 활용:**
```
CreateTenant, DeleteTenant, GetTenant, ListTenants
CreateTenantResourceAssociation, DeleteTenantResourceAssociation
UpdateReputationEntityCustomerManagedStatus (일시정지/재개)
UpdateReputationEntityPolicy (평판 정책)
SendEmail({ TenantName, ConfigurationSetName })
```

**수용 기준:**
- Callback 코드 완전 제거
- SES Console에서 테넌트별 평판/전송 통계 확인 가능
- EventBridge에서 이벤트 타입별 라우팅 동작 확인
- 이벤트 Archive에서 리플레이 테스트 통과

**롤백 전략:**
- `ses_tenant_name` NULL → 기존 로직 유지
- EventBridge Rule 비활성화 + SNS 재연결 (CDK 롤백)

---

### Phase 3: 대용량 발송 파이프라인 고도화 (7~10일)

**목표:** Quartz 순차 발송 → SQS + Lambda 병렬 발송으로 처리량 극대화

| TODO | 내용 |
|------|------|
| 3.1 | SQS `email-send-queue` + `email-send-dlq` 생성 (CDK) |
| 3.2 | email-sender Lambda를 SQS 트리거로 전환 (BatchSize 10, 동시성 제어) |
| 3.3 | Backend `EmailDispatchService` 신규: API GW → Lambda(enqueue) → SQS 등록 |
| 3.4 | SES `GetAccount` → maxSendRate 동적 조회 → SQS 동시성 자동 조정 |
| 3.5 | 배치 진행률 추적 (SQS 메시지 수 + DynamoDB 상태) |
| 3.6 | 예약 발송: EventBridge Scheduler (단건) + SQS (대량) 하이브리드 |
| 3.7 | Quartz 배치 발송 Job 제거, Quartz는 폴링/타임아웃/동기화만 담당 |
| 3.8 | DLQ 모니터링 + 재처리 Lambda 추가 |

**수용 기준:**
- SES 쿼터 대비 90% 이상 처리량 달성
- DLQ 메시지 0건 유지 (정상 상황)
- 10,000건 배치 발송 테스트 통과
- 기존 발송 API 하위 호환성 유지

**롤백 전략:**
- SQS 트리거 비활성화 + Quartz Job 재활성화

---

### Phase 4: 모니터링 / 비용 / Suppression 고도화 (4~6일)

**목표:** AWS 네이티브 데이터로 모니터링 정확도 향상

| TODO | 내용 |
|------|------|
| 4.1 | CloudWatch 테넌트별 SES 메트릭 조회 (5분 캐시) |
| 4.2 | Cost Explorer 실 비용 조회 (1일 캐시, 폴백 유지) |
| 4.3 | SES Suppression List ↔ 로컬 DB 양방향 동기화 (1일 1회) |
| 4.4 | VDM 활성화 + ISP별 전달률 인사이트 대시보드 (비용 검토 후) |
| 4.5 | 프론트엔드 대시보드에 SES 테넌트 상태/평판 정보 표시 |
| ~~4.6~~ | ~~기존 테넌트 마이그레이션 스크립트~~ (해당 없음 - 기존 테넌트 0건) |

**수용 기준:**
- CloudWatch 실시간 메트릭이 대시보드에 표시
- `/monitoring/cost`가 Cost Explorer 실 데이터 반환
- SES Suppression과 로컬 DB가 1일 단위 동기화

---

## 11. 전체 일정

| Phase | 공수 | 의존성 | 병렬 가능 |
|-------|------|--------|----------|
| Phase 1: 코드 품질 정비 | 3~5일 | 없음 | 즉시 시작 |
| Phase 2: Callback 제거 + EventBridge + Multi-Tenant | 7~10일 | Phase 1 권장 | |
| Phase 3: 대용량 발송 파이프라인 | 7~10일 | Phase 2 | |
| Phase 4: 모니터링 / 비용 / Suppression | 4~6일 | Phase 2 | Phase 3과 병렬 가능 |
| **합계** | **21~31일** | | |

```
Week 1-2:    Phase 1 (코드 품질)
Week 2-4:    Phase 2 (EventBridge + Multi-Tenant)
Week 4-6:    Phase 3 (대용량 발송) + Phase 4 (모니터링) 병렬
```

---

## 12. 비용 영향

| 항목 | 현재 | To-Be | 차이 |
|------|------|-------|------|
| SES 발송 | $0.10/1,000건 | $0.10/1,000건 | 동일 |
| SES Tenant | - | $0.005/월/테넌트 + $0.005/1,000건 | 소폭 증가 |
| EventBridge | - | $1.00/100만 이벤트 | SNS 대비 +$0.50 |
| SQS | - | $0.40/100만 요청 | 신규 |
| Lambda | 동일 | 동시성 증가 가능 | 소폭 증가 |
| VDM (선택) | - | $0.07/1,000건 | 선택적 |
| Quartz 서버 | 상시 가동 | 역할 축소 (발송 제거) | 절감 가능 |

---

## 13. 리스크 및 완화 전략

| 리스크 | 영향 | 완화 전략 |
|--------|------|----------|
| SES Multi-Tenant API 변경 | Phase 2 전체 | ses_tenant_name NULL 폴백 |
| EventBridge 이벤트 순서 미보장 | 상태 불일치 | 멱등 처리 + timestamp 비교 |
| SQS 메시지 중복 배달 | 이메일 중복 발송 | correlationId 기반 멱등성 체크 |
| 폴링 주기 변경 시 부하 | DB 부하 증가 | 점진적 단축 (5분 → 3분 → 2분) |
| 대량 발송 시 SES 쿼터 초과 | 발송 실패 | GetAccount 동적 조회 + SQS 동시성 제어 |
| IDC-AWS 네트워크 지연 | 폴링 지연 | DynamoDB HTTP API 사용 (VPN 불필요) |

---

## 14. 미결 사항

| # | 사항 | 영향 Phase | 결정 필요 시점 | 상태 |
|---|------|-----------|-------------|------|
| 1 | Suppression 교차 테넌트 정책 | Phase 4 | - | **확정** |
| 2 | VDM 도입 여부 ($0.07/1,000건) | Phase 4 | - | **확정** |
| 3 | 기존 테넌트 마이그레이션 | Phase 4 | - | **확정 (해당 없음)** |
| 4 | 폴링 주기 설정 | Phase 2 | - | **확정** |

### 확정 1: Suppression 2계층 구조

**전체 Suppression(계정 수준) + 테넌트별 Suppression** 2계층으로 운영한다.

```
전체 Suppression (계정 수준, SES Account Suppression List 동기화)
  ├── user1@example.com (Hard Bounce)
  ├── user2@example.com (Complaint)
  └── user3@example.com (Hard Bounce)
      │
      ├── Tenant A Suppression ── user1, user3 (A에서 발생)
      ├── Tenant B Suppression ── user2 (B에서 발생)
      └── Tenant C Suppression ── (없음, 하지만 발송 시 전체 목록으로 차단)
```

**동작 규칙:**

| 상황 | 동작 |
|------|------|
| Tenant A에서 bounce 발생 | 전체 Suppression + Tenant A Suppression 등록 |
| Tenant B가 같은 주소로 발송 시도 | 전체 Suppression에서 차단 + Tenant B Suppression에 자동 등록 |
| 관리자가 전체 Suppression에서 삭제 | 모든 테넌트에서 발송 가능 |
| 테넌트가 자기 Suppression에서 삭제 | 전체에 남아있으면 여전히 차단 |

**발송 시 검증:**

```
발송 요청
  │
  ├── 전체 Suppression 확인
  │     └── 존재 → 차단 + 해당 테넌트 Suppression에 자동 등록
  │
  ├── 테넌트 Suppression 확인
  │     └── 존재 → 차단
  │
  └── 통과 → SQS 등록 → 발송
```

### 확정 2: VDM 관리자 설정에서 ON/OFF

VDM은 관리자 설정 UI에서 토글로 ON/OFF 한다. 구조 변화 없이 API 1개 + 토글 1개로 구현.

**흐름:**

```
Frontend (설정 페이지) → VDM ON/OFF 토글
  │
  ▼
Backend: PUT /settings/vdm { "enabled": true }
  │
  ▼
API Gateway → Lambda → SES PutAccountVdmAttributes({ VdmEnabled: "ENABLED" })
```

**코드 변경 범위:**

| 레이어 | 변경 | 크기 |
|--------|------|------|
| Lambda | `PUT_VDM` 액션 추가 | 매우 작음 |
| Backend | `/settings/vdm` API 1개 | 매우 작음 |
| Frontend | 설정 토글 + 대시보드 조건부 ISP 인사이트 카드 | 작음 |
| CDK | IAM `ses:PutAccountVdmAttributes` 권한 | 1줄 |

**특징:**
- 즉시 ON/OFF 가능, 재배포 불필요
- 비용은 ON 상태일 때 발송 건수만 과금 ($0.07/1,000건)
- VDM OFF 시 ISP 인사이트 카드 숨김 (조건부 표시)
- Phase 4에 포함하여 구현

### 확정 3: 기존 테넌트 마이그레이션

기존 활성 테넌트가 0건이므로 마이그레이션 불필요. 신규 테넌트 생성 시 SES Tenant가 자동 생성되는 구조로 Phase 2부터 적용.

### 확정 4: 폴링 주기 관리자 설정

폴링 주기를 하드코딩하지 않고, **관리자 설정 UI에서 변경 가능**하도록 한다.

**흐름:**

```
Frontend (설정 페이지) → 폴링 주기 입력 (1~10분)
  │
  ▼
Backend: PUT /settings/polling-interval { "intervalMinutes": 2 }
  │
  ▼
Quartz ResultPollingService 트리거 주기 동적 변경
```

**코드 변경 범위:**

| 레이어 | 변경 | 크기 |
|--------|------|------|
| Backend | `/settings/polling-interval` API + Quartz 주기 동적 변경 | 작음 |
| Frontend | 설정 페이지에 주기 입력 필드 | 작음 |

**기본값:** 2분 (Callback 제거 후 적정 수준)
**허용 범위:** 1~10분 (1분 미만은 DynamoDB 비용 과다)
