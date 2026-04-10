package com.msas.tenantsetup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotEmpty(message = "email은 필수입니다.")
    @Email(message = "유효한 이메일 형식이어야 합니다.")
    private String email;
}
