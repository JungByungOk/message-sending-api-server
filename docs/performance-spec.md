# 성능 사양서 (Performance Specification)

> 이메일 발송 시스템(EMS)의 각 구간별 처리 속도, 제한 사항, 병목 지점을 정리한 문서입니다.

## 시스템 발송 흐름

```
[클라이언트] → [Backend API] → [API Gateway] → [SQS 큐] → [Lambda email-sender] → [AWS SES]
                                                                                        ↓
[Frontend 조회] ← [Backend DB] ← [Callback/Polling] ← [Lambda event-processor] ← [SNS 이벤트]
```

---

## 1. 구간별 처리 속도

### 1-1. Backend → API Gateway (SQS 큐잉)

| 항목 | 값 | 비고 |
|------|-----|------|
| RateLimiter | **`ses.max-send-rate` 설정값** (Sandbox 기본: **1 TPS**) | `SesRateLimiterConfig` 공유 Bean — `SendTemplatedEmailJob`, `SendTemplatedEmailWithPollingJob`, `EmailController` 공통 적용 |
| API Gateway Throttle | 100 TPS (burst 200) | CDK 설정 (`ems-cdk-stack.ts`) |
| 실효 처리량 (Sandbox) | **1건/초** | RateLimiter가 병목 (application.yml `ses.max-send-rate: 1.0`) |
| 실효 처리량 (Production) | `ses.max-send-rate` 설정에 따라 상이 | yml 값만 수정하면 즉시 반영 |
| 1,000건 큐잉 시간 (Sandbox 1 TPS) | ~16분 40초 | |
| 10,000건 큐잉 시간 (Sandbox 1 TPS) | ~2시간 46분 | |
| 1,000건 큐잉 시간 (Production 50 TPS) | ~20초 | |
| 10,000건 큐잉 시간 (Production 50 TPS) | ~3분 20초 | |

> **참고**: `@DisallowConcurrentExecution`으로 동일 Job 타입의 동시 실행이 불가합니다. 여러 발송 요청이 들어오면 순차 처리됩니다.
>
> **RateLimiter 구성 변경**: 기존 각 Job 클래스에 `RateLimiter.create(50.0)` 하드코딩 방식에서 `SesRateLimiterConfig.java` 공유 Bean으로 통합되었습니다. Production 전환 시 `application.yml`의 `ses.max-send-rate` 값만 변경하면 전체 발송 경로(예약 발송 + 즉시 발송)에 일괄 적용됩니다.

### 1-2. SQS → Lambda (email-sender)

| 항목 | 값 | 비고 |
|------|-----|------|
| Lambda batchSize | 10건/호출 | SQS 이벤트 소스 설정 |
| Lambda 동시성 | **최대 5개** | `reservedConcurrentExecutions: 5` |
| Lambda timeout | 30초/호출 | |
| 최대 동시 처리량 | 50건 (5 x 10) | |
| 건당 처리 시간 | ~200ms | DynamoDB 조회 2회 + SES 호출 + DynamoDB 쓰기 |
| Lambda당 배치 처리 시간 | ~2초 (10건) | |
| SQS visibility timeout | 180초 | Lambda 실패 시 재시도까지 대기 시간 |
| DLQ 재시도 횟수 | 3회 | 3회 실패 시 `ems-send-dlq`로 이동 |

### 1-3. Lambda (email-sender) → AWS SES

| 항목 | 값 | 비고 |
|------|-----|------|
| SES Sandbox sending rate | 1 TPS | 프로덕션 전환 전 |
| SES Production sending rate | **14 TPS** (기본값) | 계정별 상이, AWS 콘솔에서 확인 |
| SES 일일 발송 한도 (Sandbox) | 200건/일 | |
| SES 일일 발송 한도 (Production) | **50,000건/일** (기본값) | 증가 요청 가능 |
| 실효 발송 속도 | SES rate limit에 종속 | Lambda 동시성 5개로 SES rate limit 초과 방지 |

### 1-4. SES 이벤트 → SNS → Lambda (event-processor) → Backend

| 항목 | 값 | 비고 |
|------|-----|------|
| 이벤트 발생 지연 | 수 초 ~ 수 분 | SES 내부 처리 시간 (이벤트 타입별 상이) |
| event-processor Lambda timeout | 30초 | |
| event-processor DLQ 재시도 | 없음 (Lambda DLQ) | 14일 보관 후 삭제 |
| Callback HTTP timeout | 10초 | Backend 서버 응답 대기 |
| Backend DB 업데이트 | UPDATE 1건 + INSERT 1건/이벤트 | `UpdateFinalEmailStatus` + `InsertEmailEventLog` |
| HikariCP 커넥션 풀 | 10개 | `application.yml` 설정 |

### 1-5. 폴링 경로 (DB → Quartz)

| 항목 | 값 | 비고 |
|------|-----|------|
| 폴링 주기 | 60초 | `polling.schedule.send-email-check-time` |
| 1회 최대 조회 건수 | **280건** | `findNewEmail` SQL `LIMIT 280` |
| 동시성 제어 | `FOR UPDATE SKIP LOCKED` | 다중 인스턴스 환경에서 중복 처리 방지 |
| 폴링 → SQS 큐잉 | 280건 x 50 TPS = ~5.6초 | |

---

## 2. 시간당/일일 최대 발송량

### 2-1. 구간별 병목 분석

| 구간 | 처리 속도 | 시간당 처리량 |
|------|-----------|-------------|
| Backend → SQS 큐잉 (Sandbox) | **1 TPS** (ses.max-send-rate) | 3,600건 |
| Backend → SQS 큐잉 (Production) | `ses.max-send-rate` 설정값 | 설정값 × 3,600 |
| Lambda → SES (동시성 5) | SES rate limit 종속 | SES 종속 |
| **SES 발송 (병목)** | **14 TPS** (Production 기본) | **50,400건** |

> **시스템 병목은 SES rate limit**입니다. Sandbox 환경에서는 Backend RateLimiter(1 TPS)가 우선 병목이 되며, Production 전환 후에는 SES rate limit이 병목이 됩니다.

### 2-2. 환경별 최대 발송량

| 환경 | 시간당 최대 | 일일 최대 | 비고 |
|------|------------|-----------|------|
| **Sandbox** | 3,600건 (1 TPS) | **200건** (일일 한도) | 테스트 전용 |
| **Production (기본)** | 50,400건 (14 TPS) | **50,000건** (일일 한도) | 일일 한도에 먼저 도달 |
| Production (한도 증가) | rate × 3,600 | AWS 승인에 따라 상이 | 최대 수백 TPS 가능 |

> **주의**: Sandbox 환경에서는 일일 200건 한도가 적용되므로, 시간당 3,600건(1 TPS)이어도 실제로는 200건만 발송 가능합니다.

### 2-3. SES 한도 확인 방법

AWS 콘솔: **SES → Account dashboard → Sending statistics**
- Daily sending quota: 일일 발송 한도
- Maximum send rate: 초당 발송 한도
- Production 전환 및 한도 증가는 AWS Support에 요청 필요

---

## 3. 대용량 발송 시나리오 (10,000건)

### 3-1. 시간 예측

| 구간 | 소요 시간 | 누적 시간 |
|------|-----------|-----------|
| Backend → SQS 큐잉 (50 TPS) | ~3분 20초 | 3분 20초 |
| SQS → Lambda → SES 발송 | ~3분 30초 | ~6분 50초 |
| SES 이벤트 수신 (Delivery) | ~1분 ~ 5분 | ~8분 ~ 12분 |
| Backend DB 상태 반영 | ~수 초 | ~8분 ~ 12분 |
| **전체 소요 시간** | | **약 8~12분** |

> **참고**: SES Production rate limit 14 TPS 기준입니다. Sandbox(1 TPS)에서는 SES 구간만 ~2시간 47분이 소요됩니다.

### 3-2. 폴링 경로 (280건 제한)

폴링 경로(`QueuedEmailPoller`)로 10,000건을 처리하는 경우:

| 구간 | 계산 |
|------|------|
| 1회 폴링 최대 | 280건 |
| 필요 폴링 횟수 | 36회 (10,000 ÷ 280) |
| 폴링 주기 | 60초 |
| 전체 폴링 시간 | **~36분** |
| + SQS/SES 처리 시간 | **~40분 이상** |

---

## 4. 제한 사항 및 병목

### 4-1. 현재 알려진 제한

| 제한 사항 | 영향 | 심각도 |
|-----------|------|--------|
| Quartz JobDataMap 크기 제한 | **해결됨** — `ADM_EMAIL_SEND_BATCH` 별도 테이블 분리, JobDataMap에 batchId만 전달 | ~~Critical~~ → 해결 |
| `@DisallowConcurrentExecution` | 동시 Job 실행 불가, 순차 대기 | High |
| 폴링 경로 280건 제한 | 대용량 처리 시 다수 폴링 사이클 필요 | Medium |
| HikariCP 10 커넥션 | 대량 Callback 동시 수신 시 풀 고갈 가능 | High |
| DLQ 재처리 메커니즘 없음 | 3회 실패 메시지 14일 후 영구 유실 | Medium |
| Sandbox RateLimiter 1 TPS | 대량 발송 시 큐잉 시간 매우 길어짐 | Medium (Production 전환으로 해소) |

### 4-2. Quartz JobDataMap 크기 제한 (해결됨)

**해결 완료 (V8 마이그레이션)**: 이메일 목록을 `ADM_EMAIL_SEND_BATCH` / `ADM_EMAIL_SEND_BATCH_ITEM` 별도 테이블에 저장하고, JobDataMap에는 `batchId`만 전달합니다. `deserializeJobData()`에서 DB 조회로 복원합니다.

| 건수 | 이전 방식 (JSON 직렬화) | 현재 방식 (batchId 참조) |
|------|------------------------|------------------------|
| 100건 | ~50 KB (JobDataMap) | batchId 1건 (수 바이트) |
| 1,000건 | ~500 KB (주의) | batchId 1건 |
| 5,000건 | ~2.5 MB (위험) | batchId 1건 |
| 10,000건 | ~5 MB+ (**장애 가능**) | batchId 1건 |

### 4-3. Sending 타임아웃 자동 처리

`SendingTimeoutChecker`가 10분 주기 `@Scheduled`로 실행되어 1시간 이상 `Sending` 상태를 유지하는 레코드를 자동으로 `Timeout`으로 전환합니다.

| 항목 | 값 |
|------|-----|
| 실행 주기 | 10분 (`@Scheduled`) |
| 타임아웃 기준 | 1시간 이상 `Sending` 상태 유지 |
| 전환 상태 | `Sending` → `Timeout` |
| Terminal 여부 | 아님 — 이후 SES 이벤트 수신 시 정상 전이 가능 |
| 표시 | Frontend `StatusTag`에서 warning(주황) 스타일로 "타임아웃" 표시 |

---

## 5. SES 계정별 한도 확인

AWS 콘솔에서 확인: **SES → Account dashboard → Sending statistics**

| 항목 | Sandbox | Production (기본) | 증가 요청 후 |
|------|---------|-------------------|-------------|
| Sending rate | 1 TPS | 14 TPS | 최대 수백 TPS |
| Daily limit | 200건 | 50,000건 | 최대 수백만 건 |

> Production 전환 및 한도 증가는 AWS Support에 요청 필요합니다.

---

## 6. 최적 성능을 위한 설정 가이드

### 6-1. 소규모 발송 (일 1,000건 이하)

| 설정 | 권장값 |
|------|--------|
| RateLimiter | `ses.max-send-rate: 1.0` (Sandbox) / Production 전환 후 SES rate limit에 맞춰 조정 |
| Lambda 동시성 | 5 (현재) |
| HikariCP pool | 10 (현재) |
| 폴링 주기 | 60초 (현재) |

### 6-2. 중규모 발송 (일 10,000건)

| 설정 | 권장값 |
|------|--------|
| RateLimiter | 50 TPS |
| Lambda 동시성 | 5~10 (SES rate limit 확인 후) |
| HikariCP pool | 20 |
| 폴링 주기 | 30초 |
| JobDataMap | **해결됨** — ADM_EMAIL_SEND_BATCH 테이블로 분리, batchId만 전달 |

### 6-3. 대규모 발송 (일 100,000건 이상)

| 설정 | 권장값 |
|------|--------|
| SES 한도 증가 | AWS Support에 요청 |
| RateLimiter | `ses.max-send-rate`를 SES rate limit에 맞춰 조정 |
| Lambda 동시성 | SES rate limit / 10 (batchSize) |
| HikariCP pool | 30+ |
| 아키텍처 변경 | SQS 직접 호출 + SendMessageBatch 검토 |

---

## 7. 모니터링 포인트

| 지표 | 확인 방법 | 임계값 |
|------|-----------|--------|
| SES Bounce rate | AWS SES 대시보드 | 5% 이상 시 계정 정지 위험 |
| SES Complaint rate | AWS SES 대시보드 | 0.1% 이상 시 계정 정지 위험 |
| Lambda 에러율 | CloudWatch Metrics | 1% 이상 시 조사 필요 |
| DLQ 메시지 수 | SQS 콘솔 / CloudWatch | 0보다 클 때 즉시 확인 |
| DB 커넥션 사용률 | HikariCP 메트릭 | 80% 이상 시 pool 증가 |
| Job 실행 로그 | Backend 로그 | 실패 건수 확인 |

---

*최종 업데이트: 2026-04-09*
