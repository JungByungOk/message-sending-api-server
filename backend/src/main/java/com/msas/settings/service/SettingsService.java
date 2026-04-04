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

    private static final Map<String, String> KEY_MAP = Map.ofEntries(
            Map.entry("gatewayEndpoint", "gateway.endpoint"),
            Map.entry("gatewayRegion", "gateway.region"),
            Map.entry("gatewayAuthType", "gateway.auth-type"),
            Map.entry("gatewayApiKey", "gateway.api-key"),
            Map.entry("gatewayAccessKey", "gateway.access-key"),
            Map.entry("gatewaySecretKey", "gateway.secret-key"),
            Map.entry("gatewaySendPath", "gateway.send-path"),
            Map.entry("gatewayResultsPath", "gateway.results-path"),
            Map.entry("callbackUrl", "callback.url"),
            Map.entry("callbackSecret", "callback.secret"),
            Map.entry("deliveryMode", "delivery.mode"),
            Map.entry("pollingInterval", "delivery.polling-interval")
    );

    /**
     * 전체 설정 조회.
     */
    public AwsSettingsResponseDTO getSettings() {
        Map<String, String> configs = getAllConfigValues();

        AwsSettingsResponseDTO dto = new AwsSettingsResponseDTO();
        // Gateway
        dto.setGatewayEndpoint(configs.getOrDefault("gateway.endpoint", ""));
        dto.setGatewayRegion(configs.getOrDefault("gateway.region", "ap-northeast-2"));
        dto.setGatewayAuthType(configs.getOrDefault("gateway.auth-type", "API_KEY"));
        dto.setGatewayApiKeyMasked(maskSecret(configs.getOrDefault("gateway.api-key", "")));
        dto.setGatewayAccessKey(configs.getOrDefault("gateway.access-key", ""));
        dto.setGatewaySecretKeyMasked(maskSecret(configs.getOrDefault("gateway.secret-key", "")));
        dto.setGatewaySendPath(configs.getOrDefault("gateway.send-path", "/send-email"));
        dto.setGatewayResultsPath(configs.getOrDefault("gateway.results-path", "/results"));
        dto.setGatewayConfigPath(configs.getOrDefault("gateway.config-path", "/config"));
        dto.setGatewayConfigured(isNotBlank(configs.get("gateway.endpoint")));
        // Callback
        dto.setCallbackUrl(configs.getOrDefault("callback.url", ""));
        dto.setCallbackSecretMasked(maskSecret(configs.getOrDefault("callback.secret", "")));
        dto.setCallbackConfigured(isNotBlank(configs.get("callback.url")) && isNotBlank(configs.get("callback.secret")));
        // Delivery
        dto.setDeliveryMode(configs.getOrDefault("delivery.mode", "callback"));
        dto.setPollingInterval(configs.getOrDefault("delivery.polling-interval", "300000"));
        // Meta
        dto.setUpdatedAt(getLatestUpdatedAt());

        return dto;
    }

    /**
     * 전체 설정 저장.
     */
    public AwsSettingsResponseDTO saveSettings(AwsSettingsDTO settings) {
        saveConfig("gateway.endpoint", settings.getGatewayEndpoint(), "API Gateway Base URL", false);
        saveConfig("gateway.region", settings.getGatewayRegion(), "API Gateway AWS 리전", false);
        saveConfig("gateway.auth-type", settings.getGatewayAuthType(), "인증 방식", false);
        saveConfig("gateway.api-key", settings.getGatewayApiKey(), "API Gateway API Key", true);
        saveConfig("gateway.access-key", settings.getGatewayAccessKey(), "IAM Access Key", true);
        saveConfig("gateway.secret-key", settings.getGatewaySecretKey(), "IAM Secret Key", true);
        saveConfig("gateway.send-path", settings.getGatewaySendPath(), "이메일 발송 경로", false);
        saveConfig("gateway.results-path", settings.getGatewayResultsPath(), "발송 결과 조회 경로", false);
        saveConfig("gateway.config-path", settings.getGatewayConfigPath(), "SSM 설정 동기화 경로", false);
        saveConfig("callback.url", settings.getCallbackUrl(), "Callback URL", false);
        saveConfig("callback.secret", settings.getCallbackSecret(), "Callback Secret", true);
        saveConfig("delivery.mode", settings.getDeliveryMode(), "수신 모드", false);
        saveConfig("delivery.polling-interval", settings.getPollingInterval(), "보정 폴링 주기 (ms)", false);

        log.info("SettingsService - 설정 저장 완료. (mode: {})", settings.getDeliveryMode());

        // SSM Parameter Store 동기화 (API Gateway PUT /config 호출)
        syncToSsm(settings);

        return getSettings();
    }

    /**
     * API Gateway 연결 테스트.
     */
    public AwsTestResultDTO testConnection(AwsSettingsDTO settings) {
        if (!isNotBlank(settings.getGatewayEndpoint())) {
            return new AwsTestResultDTO(false, "Gateway Endpoint URL이 비어 있습니다.", 0);
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String testUrl = settings.getGatewayEndpoint().replaceAll("/$", "")
                    + (isNotBlank(settings.getGatewayResultsPath()) ? settings.getGatewayResultsPath() : "/results");

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            if ("API_KEY".equals(settings.getGatewayAuthType()) && isNotBlank(settings.getGatewayApiKey())) {
                requestBuilder.header("x-api-key", settings.getGatewayApiKey());
            }

            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 400) {
                return new AwsTestResultDTO(true,
                        "API Gateway 연결 성공 (HTTP " + statusCode + ")", statusCode);
            } else if (statusCode == 403) {
                return new AwsTestResultDTO(false,
                        "인증 실패 - 인증 정보를 확인하세요 (HTTP 403)", statusCode);
            } else {
                return new AwsTestResultDTO(false,
                        "연결 실패 (HTTP " + statusCode + ")", statusCode);
            }
        } catch (Exception e) {
            log.warn("SettingsService - API Gateway 연결 테스트 실패.", e);
            return new AwsTestResultDTO(false, "연결 실패: " + e.getMessage(), 0);
        }
    }

    /**
     * Callback Secret 값을 반환합니다 (검증용).
     */
    public String getCallbackSecret() {
        SystemConfigEntity entity = systemConfigRepository.findByKey("callback.secret");
        return entity != null ? entity.getConfigValue() : "";
    }

    /**
     * 현재 수신 모드를 반환합니다.
     */
    public String getDeliveryMode() {
        SystemConfigEntity entity = systemConfigRepository.findByKey("delivery.mode");
        return entity != null && isNotBlank(entity.getConfigValue()) ? entity.getConfigValue() : "callback";
    }

    /**
     * 보정 폴링 주기를 반환합니다 (ms).
     */
    public long getPollingInterval() {
        SystemConfigEntity entity = systemConfigRepository.findByKey("delivery.polling-interval");
        try {
            return entity != null && isNotBlank(entity.getConfigValue())
                    ? Long.parseLong(entity.getConfigValue()) : 300000L;
        } catch (NumberFormatException e) {
            return 300000L;
        }
    }

    // --- SSM Sync ---

    /**
     * API Gateway PUT /config 를 호출하여 SSM Parameter Store에 동기화합니다.
     * Callback URL, Callback Secret, 수신 모드를 SSM에 저장하면
     * Lambda event-processor가 30초 내 자동 반영합니다.
     */
    private void syncToSsm(AwsSettingsDTO settings) {
        Map<String, String> configs = getAllConfigValues();
        String endpoint = isNotBlank(settings.getGatewayEndpoint())
                ? settings.getGatewayEndpoint()
                : configs.getOrDefault("gateway.endpoint", "");
        String configPath = isNotBlank(settings.getGatewayConfigPath())
                ? settings.getGatewayConfigPath()
                : configs.getOrDefault("gateway.config-path", "/config");

        if (!isNotBlank(endpoint)) {
            log.warn("SettingsService - Gateway Endpoint 미설정. SSM 동기화 건너뜀.");
            return;
        }

        String configUrl = endpoint.replaceAll("/$", "") + configPath;

        try {
            // SSM에 동기화할 설정값
            String jsonBody = String.format(
                    "{\"mode\":\"%s\",\"callback_url\":\"%s\",\"callback_secret\":\"%s\"}",
                    settings.getDeliveryMode() != null ? settings.getDeliveryMode() : "callback",
                    settings.getCallbackUrl() != null ? settings.getCallbackUrl() : "",
                    settings.getCallbackSecret() != null ? settings.getCallbackSecret() : ""
            );

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(configUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));

            // 인증 헤더
            String authType = isNotBlank(settings.getGatewayAuthType())
                    ? settings.getGatewayAuthType()
                    : configs.getOrDefault("gateway.auth-type", "API_KEY");

            if ("API_KEY".equals(authType)) {
                String apiKey = isNotBlank(settings.getGatewayApiKey())
                        ? settings.getGatewayApiKey()
                        : configs.getOrDefault("gateway.api-key", "");
                if (isNotBlank(apiKey)) {
                    requestBuilder.header("x-api-key", apiKey);
                }
            }

            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("SettingsService - SSM 동기화 완료. (HTTP {})", response.statusCode());
            } else {
                log.warn("SettingsService - SSM 동기화 실패. (HTTP {}, body: {})",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("SettingsService - SSM 동기화 실패. Gateway 미구성 상태이면 정상입니다.", e);
        }
    }

    // --- Helper methods ---

    private Map<String, String> getAllConfigValues() {
        List<SystemConfigEntity> all = systemConfigRepository.findAll();
        return all.stream().collect(Collectors.toMap(
                SystemConfigEntity::getConfigKey,
                e -> e.getConfigValue() != null ? e.getConfigValue() : "",
                (a, b) -> b));
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

    private LocalDateTime getLatestUpdatedAt() {
        List<SystemConfigEntity> all = systemConfigRepository.findAll();
        return all.stream()
                .map(SystemConfigEntity::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}
