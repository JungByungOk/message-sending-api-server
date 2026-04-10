package com.msas.tenantsetup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSetupStepDTO {
    private int step;
    private String name;
    private String status;  // COMPLETED, WAITING, PENDING
}
