package com.msas.telegram.dto;

import lombok.Data;

@Data
public class RequestTelegramMessageDTO {
    String channelName = "";
    String message = "";
}