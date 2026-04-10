package com.msas.tenantsetup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationStatusDTO {
    private String email;
    private String verificationStatus; // PENDING / SUCCESS / FAILED
}
