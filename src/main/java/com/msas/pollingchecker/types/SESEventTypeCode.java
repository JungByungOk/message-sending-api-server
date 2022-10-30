package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SESEventTypeCode {

    Bounce("반송", EmailSendStatusCode.SF), // blacklist
    Complaint("수신 거부", EmailSendStatusCode.SF), // blacklist
    Delivery("전송 완료", EmailSendStatusCode.SS),
    Send("전송", EmailSendStatusCode.SF),
    Reject("거부", EmailSendStatusCode.SF), // blacklist
    Open("열기 이벤트", EmailSendStatusCode.SS),
    Click("클릭 이벤트", EmailSendStatusCode.SS),
    Failure("렌더링 오류 이벤트", EmailSendStatusCode.SF),
    DeliveryDelay("전송 지연", EmailSendStatusCode.SM),
    Subscription("구독 설정", EmailSendStatusCode.SS);

    private final String desc;
    private final EmailSendStatusCode emailSendStatusCode;
}
