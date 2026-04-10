package com.msas.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AwsSettingsDTO {
    // API Gateway
    @NotBlank(message = "Gateway 엔드포인트는 필수입니다.")
    private String gatewayEndpoint;

    @NotBlank(message = "Gateway 리전은 필수입니다.")
    private String gatewayRegion;

    private String gatewayApiKey;
    private String gatewaySendPath;
    private String gatewayResultsPath;
    private String gatewayConfigPath;
    private String gatewayTenantSetupPath;

    // Callback
    private String callbackUrl;
    private String callbackSecret;

    // Delivery
    @Pattern(regexp = "^(callback|polling)$", message = "deliveryMode는 'callback' 또는 'polling'이어야 합니다.")
    private String deliveryMode;

    private String pollingInterval;
}
