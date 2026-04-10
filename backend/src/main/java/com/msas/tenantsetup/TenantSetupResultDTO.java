package com.msas.tenantsetup;

import com.msas.ses.identity.DkimRecordsDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSetupResultDTO {
    private ResponseTenantDTO tenant;
    private DkimRecordsDTO dkimRecords;
}
