package com.msas.ses.configset;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestConfigSetDTO {

    @NotBlank(message = "테넌트 ID는 필수입니다.")
    private String tenantId;
}
