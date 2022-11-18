package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnumEmailSendDivisionCode {

    IMT("즉시발송"),
    RSV("예약발송");

    private final String desc;
}
