package com.msas.pollingchecker.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailSendStatusCode {

    SR("준비중"),
    SQ("대기중"),
    SM("처리중"),
    SS("성공"),
    SF("실패");

    private final String desc;
}
