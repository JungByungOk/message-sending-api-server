package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnumSESEventTypeCode {

    Bounce("반송", EnumEmailSendStatusCode.SF), // blacklist
    Complaint("수신 거부", EnumEmailSendStatusCode.SF), // blacklist
    Delivery("전송 완료", EnumEmailSendStatusCode.SS),
    Send("전송", EnumEmailSendStatusCode.SF),
    Reject("거부", EnumEmailSendStatusCode.SF), // blacklist
    Open("열기 이벤트", EnumEmailSendStatusCode.SS),
    Click("클릭 이벤트", EnumEmailSendStatusCode.SS),
    Failure("렌더링 오류 이벤트", EnumEmailSendStatusCode.SF),
    DeliveryDelay("전송 지연", EnumEmailSendStatusCode.SM),
    Subscription("구독 설정", EnumEmailSendStatusCode.SS);

    private final String desc;
    private final EnumEmailSendStatusCode emailSendStatusCode;
}
