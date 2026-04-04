package com.msas.onboarding;

import com.msas.ses.identity.DkimRecordsDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResultDTO {
    private ResponseTenantDTO tenant;
    private DkimRecordsDTO dkimRecords;
}
