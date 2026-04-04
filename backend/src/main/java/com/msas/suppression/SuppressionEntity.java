package com.msas.suppression;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SuppressionEntity {
    private Long id;
    private String tenantId;
    private String email;
    private String reason;   // BOUNCE, COMPLAINT
    private LocalDateTime createdAt;
}
