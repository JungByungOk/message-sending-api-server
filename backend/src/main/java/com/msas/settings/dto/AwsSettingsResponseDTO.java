package com.msas.settings.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AwsSettingsResponseDTO {
    // API Gateway
    private String gatewayEndpoint;
    private String gatewayRegion;
    private String gatewayApiKeyMasked;
    private String gatewaySendPath;
    private String gatewayResultsPath;
    private String gatewayConfigPath;
    private String gatewayTenantSetupPath;
    private boolean gatewayConfigured;
    // Callback
    private String callbackUrl;
    private String callbackSecretMasked;
    private boolean callbackConfigured;
    // Delivery
    private String deliveryMode;
    private String pollingInterval;
    // Meta
    private LocalDateTime updatedAt;
}
