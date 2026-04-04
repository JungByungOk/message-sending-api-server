package com.msas.tenant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantSenderEntity {
    private Long id;
    private String tenantId;
    private String email;
    private String displayName;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
