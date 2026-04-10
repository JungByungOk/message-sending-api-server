package com.msas.tenant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantEntity {

    private String tenantId;
    private String tenantName;
    private String domain;
    private String apiKey;
    private String configSetName;
    private String sesTenantName;
    private String verificationStatus;
    private Integer quotaDaily;
    private Integer quotaMonthly;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
