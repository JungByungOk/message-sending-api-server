package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnumEmailSendStatusCode {

    SR("준비중"),  // DB에 등록된 상태
    SQ("대기중"),  // 스케쥴러 쿼츠로 전달된 상태
    SM("처리중"),  // AWS SES 메일 발송된 상태
    SS("성공"),   // 결과
    SF("실패");   // 결과

    private final String desc;
}
