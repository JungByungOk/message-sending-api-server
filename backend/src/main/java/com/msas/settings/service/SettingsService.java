package com.msas.settings.service;

import com.msas.settings.dto.AwsSettingsDTO;
import com.msas.settings.dto.AwsSettingsResponseDTO;
import com.msas.settings.dto.AwsTestResultDTO;
import com.msas.settings.entity.SystemConfigEntity;
import com.msas.settings.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettingsService {

    private final SystemConfigRepository systemConfigRepository;

    /**
     * API Gateway 설정 조회.
     */
    public AwsSettingsResponseDTO getAwsSettings() {
        Map<String, SystemConfigEntity> configs = getDbConfigMap();

        AwsSettingsResponseDTO dto = new AwsSettingsResponseDTO();
        dto.setEndpoint(getValue(configs, "gateway.endpoint"));
        dto.setRegion(getValue(configs, "gateway.region"));
        dto.setAuthType(getValueOrDefault(configs, "gateway.auth-type", "API_KEY"));
        dto.setApiKeyMasked(maskSecret(getValue(configs, "gateway.api-key")));
        dto.setAccessKey(getValue(configs, "gateway.access-key"));
        dto.setSecretKeyMasked(maskSecret(getValue(configs, "gateway.secret-key")));
        dto.setConfigured(isNotBlank(getValue(configs, "gateway.endpoint")));
        dto.setSource(hasDbValues(configs) ? "database" : "none");
        dto.setUpdatedAt(getLatestUpdatedAt(configs));

        return dto;
    }

    /**
     * API Gateway 설정 저장.
     */
    public AwsSettingsResponseDTO saveAwsSettings(AwsSettingsDTO settings) {
        saveConfig("gateway.endpoint", settings.getEndpoint(), "API Gateway Endpoint URL", false);
        saveConfig("gateway.region", settings.getRegion(), "API Gateway AWS 리전", false);
        saveConfig("gateway.auth-type", settings.getAuthType(), "인증 방식 (API_KEY/IAM)", false);
        saveConfig("gateway.api-key", settings.getApiKey(), "API Gateway API Key", true);
        saveConfig("gateway.access-key", settings.getAccessKey(), "IAM Access Key", true);
        saveConfig("gateway.secret-key", settings.getSecretKey(), "IAM Secret Key", true);

        log.info("SettingsService - API Gateway 설정 저장 완료.");
        return getAwsSettings();
    }

    /**
     * API Gateway 연결 테스트.
     * Endpoint에 GET 요청을 보내 응답을 확인합니다.
     */
    public AwsTestResultDTO testAwsConnection(AwsSettingsDTO settings) {
        if (!isNotBlank(settings.getEndpoint())) {
            return new AwsTestResultDTO(false, "Endpoint URL이 비어 있습니다.", 0);
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(settings.getEndpoint()))
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            // 인증 헤더 추가
            if ("API_KEY".equals(settings.getAuthType()) && isNotBlank(settings.getApiKey())) {
                requestBuilder.header("x-api-key", settings.getApiKey());
            }

            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 400) {
                return new AwsTestResultDTO(true,
                        "API Gateway 연결 성공 (HTTP " + statusCode + ")", statusCode);
            } else if (statusCode == 403) {
                return new AwsTestResultDTO(false,
                        "인증 실패 - API Key 또는 IAM 자격 증명을 확인하세요 (HTTP 403)", statusCode);
            } else {
                return new AwsTestResultDTO(false,
                        "연결 실패 (HTTP " + statusCode + ")", statusCode);
            }
        } catch (Exception e) {
            log.warn("SettingsService - API Gateway 연결 테스트 실패.", e);
            return new AwsTestResultDTO(false,
                    "연결 실패: " + e.getMessage(), 0);
        }
    }

    /**
     * 현재 유효한 API Gateway 설정을 반환합니다.
     */
    public AwsSettingsDTO getEffectiveSettings() {
        Map<String, SystemConfigEntity> configs = getDbConfigMap();

        AwsSettingsDTO dto = new AwsSettingsDTO();
        dto.setEndpoint(getValue(configs, "gateway.endpoint"));
        dto.setRegion(getValue(configs, "gateway.region"));
        dto.setAuthType(getValueOrDefault(configs, "gateway.auth-type", "API_KEY"));
        dto.setApiKey(getValue(configs, "gateway.api-key"));
        dto.setAccessKey(getValue(configs, "gateway.access-key"));
        dto.setSecretKey(getValue(configs, "gateway.secret-key"));
        return dto;
    }

    // --- Helper methods ---

    private Map<String, SystemConfigEntity> getDbConfigMap() {
        List<SystemConfigEntity> configs = systemConfigRepository.findByKeyPrefix("gateway.");
        return configs.stream()
                .collect(Collectors.toMap(SystemConfigEntity::getConfigKey, e -> e));
    }

    private String getValue(Map<String, SystemConfigEntity> configs, String key) {
        SystemConfigEntity entity = configs.get(key);
        return entity != null && entity.getConfigValue() != null ? entity.getConfigValue() : "";
    }

    private String getValueOrDefault(Map<String, SystemConfigEntity> configs, String key, String defaultValue) {
        String value = getValue(configs, key);
        return isNotBlank(value) ? value : defaultValue;
    }

    private void saveConfig(String key, String value, String description, boolean encrypted) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey(key);
        entity.setConfigValue(value != null ? value : "");
        entity.setDescription(description);
        entity.setEncrypted(encrypted);
        systemConfigRepository.upsert(entity);
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) return "";
        if (secret.length() <= 4) return "****";
        return secret.substring(0, 4) + "****";
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private boolean hasDbValues(Map<String, SystemConfigEntity> configs) {
        return configs.values().stream()
                .anyMatch(e -> isNotBlank(e.getConfigValue()));
    }

    private LocalDateTime getLatestUpdatedAt(Map<String, SystemConfigEntity> configs) {
        return configs.values().stream()
                .map(SystemConfigEntity::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}
