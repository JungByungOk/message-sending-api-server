package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnumEmailSendStatusCode {

    SQ("스케줄러 큐 진입"),        // 레거시 상태 코드 (폴링 방식)
    Queued("발송 큐 진입"),       // Quartz 스케줄링 완료
    Sending("SES API 호출 완료"), // SES로 발송 요청 완료, 결과 대기 중
    Delayed("일시적 전달 지연"),   // DeliveryDelay 이벤트 수신
    Delivered("수신 MTA 전달 확인"), // Delivery 이벤트 수신 (Terminal)
    Bounced("반송"),              // Bounce 이벤트 수신 (Terminal)
    Complained("수신자 스팸 신고"), // Complaint 이벤트 수신 (Terminal)
    Rejected("SES 발송 거부"),    // Reject 이벤트 수신 (Terminal)
    Error("시스템 오류"),          // RenderingFailure / SESFail / QuartzFail (Terminal)
    Blocked("내부 차단"),          // Blacklist / Suppression (Terminal)
    Timeout("SES 이벤트 미도달 타임아웃"); // 1시간 초과 Sending → Timeout 자동 전환 (Terminal)

    private final String desc;
}
