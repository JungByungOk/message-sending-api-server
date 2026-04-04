package com.msas.settings.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AwsSettingsResponseDTO {
    private String endpoint;
    private String region;
    private String authType;       // API_KEY or IAM
    private String apiKeyMasked;
    private String accessKey;
    private String secretKeyMasked;
    private boolean configured;
    private String source;         // "database" or "environment"
    private LocalDateTime updatedAt;
}
