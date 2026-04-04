package com.msas.settings.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AWS 설정 조회 응답 DTO.
 * Secret Key는 마스킹 처리되어 반환됩니다.
 */
@Data
public class AwsSettingsResponseDTO {
    // SES
    private String sesRegion;
    private String sesAccessKey;
    private String sesSecretKeyMasked;
    private boolean sesConfigured;
    // DynamoDB
    private String dynamoRegion;
    private String dynamoAccessKey;
    private String dynamoSecretKeyMasked;
    private boolean dynamoConfigured;
    // Common
    private String endpoint;
    // Meta
    private String source; // "database" or "environment"
    private LocalDateTime updatedAt;
}
