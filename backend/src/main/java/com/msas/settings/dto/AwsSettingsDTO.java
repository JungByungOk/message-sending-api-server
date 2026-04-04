package com.msas.settings.dto;

import lombok.Data;

@Data
public class AwsSettingsDTO {
    private String endpoint;
    private String region;
    private String authType;   // API_KEY or IAM
    private String apiKey;
    private String accessKey;
    private String secretKey;
}
