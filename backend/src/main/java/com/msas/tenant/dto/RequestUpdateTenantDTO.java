package com.msas.tenant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestUpdateTenantDTO {

    @Size(min = 1, max = 100, message = "테넌트 이름은 1~100자여야 합니다.")
    private String tenantName;

    @Min(value = 1, message = "일일 할당량은 1 이상이어야 합니다.")
    private Integer quotaDaily;

    @Min(value = 1, message = "월간 할당량은 1 이상이어야 합니다.")
    private Integer quotaMonthly;
}
