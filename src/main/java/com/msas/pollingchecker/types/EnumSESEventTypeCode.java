package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnumSESEventTypeCode {

    // 이하, 아마존 SES 전송 결과
    //-----------------------
    Send("전송", EnumEmailSendStatusCode.SM),

    Delivery("전송 완료", EnumEmailSendStatusCode.SS),

    Open("열기 이벤트", EnumEmailSendStatusCode.SS),
    Click("클릭 이벤트", EnumEmailSendStatusCode.SS),

    Bounce("반송", EnumEmailSendStatusCode.SF), // blacklist
    Complaint("수신 거부", EnumEmailSendStatusCode.SF), // blacklist
    Reject("거부", EnumEmailSendStatusCode.SF), // blacklist

    RenderingFailure("렌더링 오류 이벤트", EnumEmailSendStatusCode.SF),
    DeliveryDelay("전송 지연", EnumEmailSendStatusCode.SM),
    Subscription("구독 설정", EnumEmailSendStatusCode.SS),

    // custom - 내부 전송 실패 또는 전송되지 않은 사유
    //----------------------------------------
    Blacklist("이메일 주소가 블랙리스트에 등록되어 있어서 필터링 (메일 전송 제외됨)", EnumEmailSendStatusCode.SF),
    SESFail("SES 전송 단계에서 전송 불가한 상태", EnumEmailSendStatusCode.SF),
    QuartzFail("스케쥴러로 전송 등록이 불가한 상태", EnumEmailSendStatusCode.SF);

    private final String desc;
    private final EnumEmailSendStatusCode emailSendStatusCode;
}
