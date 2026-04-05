package com.msas.settings.dto;

import lombok.Data;

@Data
public class AwsSettingsDTO {
    // API Gateway
    private String gatewayEndpoint;
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
    private String deliveryMode;      // callback or polling
    private String pollingInterval;   // ms
}
