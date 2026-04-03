package com.msas.tenant.dto;

import lombok.Data;

@Data
public class RequestUpdateTenantDTO {

    private String tenantName;
    private Integer quotaDaily;
    private Integer quotaMonthly;
}
