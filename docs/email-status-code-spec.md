# 이메일 발송 상태 코드 명세

> 최종 업데이트: 2026-04-09

---

## 1. send_sts_cd (발송 상태 — Lifecycle)

이메일 발송 생애주기의 **현재 상태**를 나타냅니다.

| 코드 | 구분 | 설명 | Terminal |
|---|---|---|:---:|
| `Queued` | 진행중 | 발송 큐 진입 — Quartz 스케줄링 완료 | |
| `Sending` | 진행중 | SES API 호출 완료 — 결과 대기 중 | |
| `Delayed` | 진행중 | 일시적 전달 지연 (DeliveryDelay 이벤트) | |
| `Timeout` | 진행중 | 1시간 이상 Sending 유지 — 자동 타임아웃 전환 | |
| `Delivered` | 최종(성공) | 수신 MTA 전달 확인 (Delivery 이벤트) | ✅ |
| `Bounced` | 최종(실패) | 반송 — Hard/Soft Bounce | ✅ |
| `Complained` | 최종(실패) | 수신자 스팸 신고 (Complaint) | ✅ |
| `Rejected` | 최종(실패) | SES 발송 거부 (Reject) | ✅ |
| `Error` | 최종(실패) | 시스템 오류 (RenderingFailure / SESFail / QuartzFail) | ✅ |
| `Blocked` | 최종(실패) | 내부 차단 (Blacklist / Suppression) | ✅ |

### Terminal 상태 규칙
- Terminal 상태(`Delivered`, `Bounced`, `Complained`, `Rejected`, `Error`, `Blocked`)에 도달하면 `send_sts_cd`는 더 이상 변경하지 않는다.
- 예외: `Delivered` 이후 Complaint 수신 시 → `Complained`로 전이 (유일한 예외)
- `Open` / `Click` 이벤트는 `send_sts_cd`를 변경하지 않는다 (Engagement 이벤트로 별도 추적).
- `Timeout`은 Terminal이 아닌 진행중 상태이며, 이후 SES 이벤트(Delivery, Bounce 등) 수신 시 정상 전이됩니다. `SendingTimeoutChecker`가 10분 주기로 1시간 이상 `Sending` 상태인 레코드를 자동 전환합니다.

---

## 2. send_rslt_typ_cd (결과 유형 — SES 이벤트 원본 보존)

SES 이벤트 원본 값을 **그대로** 저장합니다.

| 코드 | 출처 | 설명 |
|---|---|---|
| `Send` | SES | SES 발송 수락 |
| `Delivery` | SES | 수신 MTA 전달 성공 |
| `Open` | SES | 수신자 이메일 열람 (트래킹 픽셀) |
| `Click` | SES | 수신자 링크 클릭 |
| `Bounce` | SES | 반송 (하위 유형: Hard / Soft / Undetermined) |
| `Complaint` | SES | 수신자 스팸 신고 |
| `Reject` | SES | SES 정책에 의한 발송 거부 |
| `RenderingFailure` | SES | 템플릿 렌더링 실패 |
| `DeliveryDelay` | SES | 일시적 전달 지연 |
| `Blacklist` | 내부 | 내부 수신거부/블랙리스트 차단 |
| `SESFail` | 내부 | SES API 호출 실패 (네트워크, 권한 등) |
| `QuartzFail` | 내부 | 스케줄러 실행 실패 |

---

## 3. 이벤트 → 상태 전이 매핑

| SES / 내부 이벤트 | send_rslt_typ_cd | send_sts_cd 전이 | 비고 |
|---|---|---|---|
| (큐 진입) | — | → `Queued` | 내부 스케줄링 시점 |
| Send | `Send` | `Queued` → `Sending` | |
| Delivery | `Delivery` | `Sending` → `Delivered` | ✅ Terminal |
| Open | `Open` | `Delivered` 유지 | Engagement 이벤트 |
| Click | `Click` | `Delivered` 유지 | Engagement 이벤트 |
| Bounce | `Bounce` | `Sending` → `Bounced` | ❌ Terminal, 블랙리스트 추가 |
| Complaint | `Complaint` | `Delivered` → `Complained` | ❌ Terminal, 블랙리스트 추가 |
| Reject | `Reject` | `Sending` → `Rejected` | ❌ Terminal |
| RenderingFailure | `RenderingFailure` | `Sending` → `Error` | ❌ Terminal |
| DeliveryDelay | `DeliveryDelay` | `Sending` → `Delayed` | 재시도 후 Delivered 또는 Bounced 전이 |
| (내부) Blacklist | `Blacklist` | `Queued` → `Blocked` | ❌ Terminal, 발송 전 차단 |
| (내부) SESFail | `SESFail` | `Queued` → `Error` | ❌ Terminal |
| (내부) QuartzFail | `QuartzFail` | `Queued` → `Error` | ❌ Terminal |
| (내부) SendingTimeout | — | `Sending` → `Timeout` | 1시간 이상 Sending 유지 시 자동 전환 (SendingTimeoutChecker) |

---

## 4. 정상 흐름 다이어그램

```
Queued → Sending → Delivered          (정상)
Queued → Sending → Delayed → Delivered (지연 후 성공)
Queued → Sending → Delayed → Bounced  (지연 후 반송)
Queued → Sending → Bounced            (즉시 반송)
Queued → Sending → Rejected           (SES 거부)
Queued → Sending → Error              (렌더링 오류 / SES 실패)
Queued → Sending → Timeout            (1시간 이상 응답 없음 — 자동 전환)
Queued → Blocked                      (블랙리스트 차단)
Queued → Error                        (스케줄러 실패)
Delivered → Complained                (전달 후 스팸 신고)
```

---

## 5. Engagement 이벤트 추적 (Open / Click)

`send_sts_cd`는 변경하지 않고 별도 컬럼 또는 이벤트 이력 테이블에서 추적합니다.

| 필드명 | 타입 | 설명 |
|---|---|---|
| `first_open_at` | TIMESTAMP | 최초 열람 시각 |
| `last_open_at` | TIMESTAMP | 최종 열람 시각 |
| `open_cnt` | INT | 열람 횟수 |
| `first_click_at` | TIMESTAMP | 최초 클릭 시각 |
| `last_click_at` | TIMESTAMP | 최종 클릭 시각 |
| `click_cnt` | INT | 클릭 횟수 |

별도 이벤트 이력 테이블 `ADM_EMAIL_EVENT_LOG`에서 모든 SES 이벤트를 추적합니다.

```sql
CREATE TABLE ADM_EMAIL_EVENT_LOG (
    EVENT_SEQ           BIGSERIAL    PRIMARY KEY,
    CORRELATION_ID      VARCHAR(36)  NULL,        -- Backend 생성 추적 ID (UUID)
    SES_MESSAGE_ID      VARCHAR(255) NULL,         -- SES 실제 메시지 ID
    TENANT_ID           VARCHAR(50)  NULL,
    EVENT_TYPE          VARCHAR(20)  NOT NULL,    -- Delivery, Bounce, Open, Click 등
    EVENT_DT            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    RECIPIENTS          TEXT         NULL,         -- JSON 배열
    EXTRA_DATA          TEXT         NULL,         -- click link 등, JSON
    STM_FIR_REG_DT      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    STM_FIR_REG_USER_ID VARCHAR(50)  NULL
);
```

모든 SES 이벤트(Delivery, Bounce, Open, Click 등)가 `SESCallbackService` 및 `ResultPollingService`에서 수신될 때마다 이 테이블에 기록됩니다.

---

## 6. 기존 코드 → 신규 코드 마이그레이션

| 기존 send_sts_cd | 신규 send_sts_cd | 조건 |
|---|---|---|
| `SR` (준비중) | `Queued` | 동일 의미 |
| `SQ` (대기중) | `Queued` | 동일 의미 (SR=SQ 통합) |
| `SM` (처리중) | `Sending` | |
| `SS` (성공) | `Delivered` | Delivery/Open/Click 이벤트 |
| `SF` (실패) | `Bounced` / `Complained` / `Rejected` / `Error` / `Blocked` | send_rslt_typ_cd 기준 분기 |
| `SD` | `Delivered` | Callback 서비스 사용 코드 |
| `SB` | `Bounced` | Callback 서비스 사용 코드 |
| `SC` | `Complained` | Callback 서비스 사용 코드 |

### DB 마이그레이션 SQL

```sql
-- 운영 적용 전 반드시 백업할 것

-- Step 1: 컬럼 크기 확장 및 DEFAULT 변경 (신규 코드 최장 값: RenderingFailure=16자)
ALTER TABLE ADM_EMAIL_SEND_DTL ALTER COLUMN SEND_STS_CD TYPE VARCHAR(20);
ALTER TABLE ADM_EMAIL_SEND_DTL ALTER COLUMN SEND_STS_CD SET DEFAULT 'Queued';
ALTER TABLE ADM_EMAIL_SEND_DTL ALTER COLUMN SEND_RSLT_TYP_CD TYPE VARCHAR(20);
COMMENT ON COLUMN ADM_EMAIL_SEND_DTL.SEND_STS_CD IS '발송 상태 코드 (Queued/Sending/Delayed/Delivered/Bounced/Complained/Rejected/Error/Blocked)';

-- Step 2: ses_msg_id → correlation_id 컬럼 리네임 + ses_message_id 별도 추가
-- (기존 컬럼명이 SES_MSG_ID인 경우)
ALTER TABLE ADM_EMAIL_SEND_DTL RENAME COLUMN SES_MSG_ID TO CORRELATION_ID;
ALTER TABLE ADM_EMAIL_SEND_DTL ALTER COLUMN CORRELATION_ID TYPE VARCHAR(36);
ALTER TABLE ADM_EMAIL_SEND_DTL ADD COLUMN IF NOT EXISTS SES_MESSAGE_ID VARCHAR(255);
COMMENT ON COLUMN ADM_EMAIL_SEND_DTL.CORRELATION_ID IS 'Backend 생성 추적 UUID — event-processor에서 SES EmailTag로 전달받아 결과 매칭에 사용';
COMMENT ON COLUMN ADM_EMAIL_SEND_DTL.SES_MESSAGE_ID IS 'SES가 발급한 실제 메시지 ID — 최종 결과 수신 시 업데이트';

-- Step 3: 기존 발송 상태 코드 일괄 변환
UPDATE ADM_EMAIL_SEND_DTL
SET send_sts_cd = CASE send_sts_cd
    WHEN 'SR' THEN 'Queued'
    WHEN 'SQ' THEN 'Queued'
    WHEN 'SM' THEN 'Sending'
    WHEN 'SS' THEN 'Delivered'
    WHEN 'SD' THEN 'Delivered'
    WHEN 'SB' THEN 'Bounced'
    WHEN 'SC' THEN 'Complained'
    WHEN 'SF' THEN CASE
        WHEN send_rslt_typ_cd = 'Bounce'     THEN 'Bounced'
        WHEN send_rslt_typ_cd = 'Complaint'  THEN 'Complained'
        WHEN send_rslt_typ_cd = 'Reject'     THEN 'Rejected'
        WHEN send_rslt_typ_cd = 'Blacklist'  THEN 'Blocked'
        ELSE 'Error'
    END
    ELSE send_sts_cd
END
WHERE send_sts_cd IN ('SR', 'SQ', 'SM', 'SS', 'SF', 'SD', 'SB', 'SC');
```

---

## 7. 구현 위치

| 파일 | 역할 |
|---|---|
| `EnumEmailSendStatusCode.java` | send_sts_cd 전체 코드 정의 (Timeout 포함) |
| `EnumSESEventTypeCode.java` | SES 이벤트 → send_sts_cd 매핑 |
| `SendingTimeoutChecker.java` | 10분 주기 @Scheduled — 1시간 이상 Sending 상태를 Timeout으로 자동 전환 |
| `SESCallbackService.java` | Callback 모드 이벤트 처리 + ADM_EMAIL_EVENT_LOG 기록 |
| `ResultPollingService.java` | DynamoDB 폴링 → 로컬 DB 업데이트 + ADM_EMAIL_EVENT_LOG 기록 |
| `SendTemplatedEmailWithPollingJob.java` | Quartz Job 발송 + correlationId 생성 및 EmailTag 주입 |
| `SendTemplatedEmailJob.java` | Quartz Job 발송 (Polling 모드) + correlationId 생성 |
| `PollingNewEmailFromNFTDB.java` | 신규 이메일 감지 및 Queued 상태 전이 |
| `EmailController.java` | 직접 발송 API + correlationId 생성 및 EmailTag 주입 |
| `SESCallbackEventDTO.java` | Callback 이벤트 DTO (correlationId + messageId 분리) |
| `EmailResultDetailDTO.java` | 발송 결과 상세 DTO (correlationId + sesMessageId 분리) |
| `SESMariaDBRepository.java` | UpdateFinalEmailStatus (correlation_id 매칭, Terminal 상태 보호) |
| `mybatis-mapper.xml` | Queued 상태 조회 쿼리 + UpdateFinalEmailStatus + InsertEmailEventLog |
| `tenant-mapper.xml` | 발송 카운트 집계 쿼리 |
| `V1__init_tables.sql` | ADM_EMAIL_EVENT_LOG 테이블 DDL |

### Correlation ID 흐름

```
[Backend] UUID 생성 → correlation_id 로컬 DB 저장
    ↓
[Backend → SES] EmailTag: { name: "correlation_id", value: "<uuid>" }
    ↓
[SES → SNS → Lambda event-processor] EmailTag에서 correlationId 추출
    ↓
[Lambda → DynamoDB] correlationId 포함 저장
[Lambda → ESM Callback] correlationId 포함 전송 (Callback 모드)
    ↓
[ESM] correlationId로 ADM_EMAIL_SEND_DTL WHERE correlation_id = ? 매칭 (1-hop)
```

- **문제 배경**: Backend가 SQS에 저장한 메시지 ID와 SES가 발급한 메시지 ID가 달라 직접 매칭 불가
- **해결**: Backend가 correlation_id(UUID)를 생성하여 SES EmailTag로 전달 → event-processor가 추출 → 콜백/폴링 결과에 포함 → 로컬 DB 1-hop 매칭
