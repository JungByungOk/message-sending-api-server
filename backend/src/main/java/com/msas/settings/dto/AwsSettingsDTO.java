package com.msas.settings.dto;

import lombok.Data;

@Data
public class AwsSettingsDTO {
    // SES
    private String sesRegion;
    private String sesAccessKey;
    private String sesSecretKey;
    // DynamoDB
    private String dynamoRegion;
    private String dynamoAccessKey;
    private String dynamoSecretKey;
    // Common
    private String endpoint;
}
