package com.msas.tenant.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RequestCreateTenantDTO {

    @NotEmpty(message = "tenantName is required")
    private String tenantName;

    @NotEmpty(message = "domain is required")
    private String domain;

    private Integer quotaDaily;
    private Integer quotaMonthly;
}
