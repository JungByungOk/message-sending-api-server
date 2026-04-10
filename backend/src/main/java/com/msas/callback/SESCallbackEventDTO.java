package com.msas.callback;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SESCallbackEventDTO {
    private String tenantId;
    private String messageId;
    private String correlationId;
    private String eventType;   // DELIVERY, BOUNCE, COMPLAINT
    private LocalDateTime timestamp;
    private List<String> recipients;
    private Map<String, String> details;
}
