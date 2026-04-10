package com.msas.tenantsetup;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TenantSetupStartRequest {

    @NotEmpty(message = "tenantName은 필수입니다.")
    private String tenantName;

    @NotEmpty(message = "domain은 필수입니다.")
    private String domain;

    private String contactEmail;
}
