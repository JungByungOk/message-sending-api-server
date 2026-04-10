package com.msas.tenantsetup;

import lombok.Data;

import java.util.List;

@Data
public class TenantSetupStatusDTO {
    private String tenantId;
    private String domain;
    private List<TenantSetupStepDTO> steps;
    private String verificationStatus;
    private String tenantStatus;
}
