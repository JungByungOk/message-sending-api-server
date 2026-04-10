package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnumSESEventTypeCode {

    // 이하, 아마존 SES 전송 결과
    //-----------------------
    Send("전송", EnumEmailSendStatusCode.Sending),

    Delivery("전송 완료", EnumEmailSendStatusCode.Delivered),

    Open("열기 이벤트", EnumEmailSendStatusCode.Delivered),   // send_sts_cd 변경 없음 (Delivered 유지, 멱등)
    Click("클릭 이벤트", EnumEmailSendStatusCode.Delivered),  // send_sts_cd 변경 없음 (Delivered 유지, 멱등)

    Bounce("반송", EnumEmailSendStatusCode.Bounced),
    Complaint("수신 거부", EnumEmailSendStatusCode.Complained),
    Reject("거부", EnumEmailSendStatusCode.Rejected),

    RenderingFailure("렌더링 오류 이벤트", EnumEmailSendStatusCode.Error),
    DeliveryDelay("전송 지연", EnumEmailSendStatusCode.Delayed),
    Subscription("구독 설정", EnumEmailSendStatusCode.Delivered),

    // custom - 내부 전송 실패 또는 전송되지 않은 사유
    //----------------------------------------
    Blacklist("이메일 주소가 블랙리스트에 등록되어 있어서 필터링 (메일 전송 제외됨)", EnumEmailSendStatusCode.Blocked),
    SESFail("SES 전송 단계에서 전송 불가한 상태", EnumEmailSendStatusCode.Error),
    QuartzFail("스케쥴러로 전송 등록이 불가한 상태", EnumEmailSendStatusCode.Error);

    private final String desc;
    private final EnumEmailSendStatusCode emailSendStatusCode;
}
