package com.msas.settings.service;

import com.msas.settings.dto.AwsSettingsDTO;
import com.msas.settings.dto.AwsSettingsResponseDTO;
import com.msas.settings.dto.AwsTestResultDTO;
import com.msas.settings.entity.SystemConfigEntity;
import com.msas.settings.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettingsService {

    private final SystemConfigRepository systemConfigRepository;
    private final AwsClientProvider awsClientProvider;

    @Value("${aws.ses.region:}")
    private String envSesRegion;
    @Value("${aws.ses.access-key:}")
    private String envSesAccessKey;
    @Value("${aws.ses.secret-key:}")
    private String envSesSecretKey;
    @Value("${aws.dynamo.region:}")
    private String envDynamoRegion;
    @Value("${aws.dynamo.access-key:}")
    private String envDynamoAccessKey;
    @Value("${aws.dynamo.secret-key:}")
    private String envDynamoSecretKey;
    @Value("${aws.endpoint:}")
    private String envEndpoint;

    /**
     * AWS 설정 조회. DB 값이 있으면 DB 우선, 없으면 환경변수 fallback.
     */
    public AwsSettingsResponseDTO getAwsSettings() {
        Map<String, SystemConfigEntity> dbConfigs = getDbConfigMap();

        AwsSettingsResponseDTO dto = new AwsSettingsResponseDTO();

        String sesRegion = resolveValue(dbConfigs, "aws.ses.region", envSesRegion);
        String sesAccessKey = resolveValue(dbConfigs, "aws.ses.access-key", envSesAccessKey);
        String sesSecretKey = resolveValue(dbConfigs, "aws.ses.secret-key", envSesSecretKey);
        String dynamoRegion = resolveValue(dbConfigs, "aws.dynamo.region", envDynamoRegion);
        String dynamoAccessKey = resolveValue(dbConfigs, "aws.dynamo.access-key", envDynamoAccessKey);
        String dynamoSecretKey = resolveValue(dbConfigs, "aws.dynamo.secret-key", envDynamoSecretKey);
        String endpoint = resolveValue(dbConfigs, "aws.endpoint", envEndpoint);

        dto.setSesRegion(sesRegion);
        dto.setSesAccessKey(sesAccessKey);
        dto.setSesSecretKeyMasked(maskSecret(sesSecretKey));
        dto.setSesConfigured(isNotBlank(sesAccessKey) && isNotBlank(sesSecretKey));

        dto.setDynamoRegion(dynamoRegion);
        dto.setDynamoAccessKey(dynamoAccessKey);
        dto.setDynamoSecretKeyMasked(maskSecret(dynamoSecretKey));
        dto.setDynamoConfigured(isNotBlank(dynamoAccessKey) && isNotBlank(dynamoSecretKey));

        dto.setEndpoint(endpoint);
        dto.setSource(hasDbValues(dbConfigs) ? "database" : "environment");
        dto.setUpdatedAt(getLatestUpdatedAt(dbConfigs));

        return dto;
    }

    /**
     * AWS 설정 저장.
     */
    public AwsSettingsResponseDTO saveAwsSettings(AwsSettingsDTO settings) {
        saveConfig("aws.ses.region", settings.getSesRegion(), "AWS SES 리전", false);
        saveConfig("aws.ses.access-key", settings.getSesAccessKey(), "AWS SES Access Key", true);
        saveConfig("aws.ses.secret-key", settings.getSesSecretKey(), "AWS SES Secret Key", true);
        saveConfig("aws.dynamo.region", settings.getDynamoRegion(), "AWS DynamoDB 리전", false);
        saveConfig("aws.dynamo.access-key", settings.getDynamoAccessKey(), "AWS DynamoDB Access Key", true);
        saveConfig("aws.dynamo.secret-key", settings.getDynamoSecretKey(), "AWS DynamoDB Secret Key", true);
        saveConfig("aws.endpoint", settings.getEndpoint(), "AWS Endpoint Override", false);

        log.info("SettingsService - AWS 설정 저장 완료.");

        // AWS 클라이언트 재생성
        awsClientProvider.refresh(getEffectiveSettings());

        return getAwsSettings();
    }

    /**
     * AWS 연결 테스트.
     */
    public AwsTestResultDTO testAwsConnection(AwsSettingsDTO settings) {
        AwsTestResultDTO result = new AwsTestResultDTO();

        // SES 테스트
        try {
            if (isNotBlank(settings.getSesAccessKey()) && isNotBlank(settings.getSesSecretKey())) {
                SesV2Client testClient = buildSesV2Client(
                        settings.getSesRegion(), settings.getSesAccessKey(),
                        settings.getSesSecretKey(), settings.getEndpoint());
                testClient.getAccount();
                testClient.close();
                result.setSesConnected(true);
                result.setSesMessage("SES 연결 성공");
            } else {
                result.setSesConnected(false);
                result.setSesMessage("Access Key 또는 Secret Key가 비어 있습니다.");
            }
        } catch (Exception e) {
            result.setSesConnected(false);
            result.setSesMessage("SES 연결 실패: " + e.getMessage());
            log.warn("SettingsService - SES 연결 테스트 실패.", e);
        }

        // DynamoDB 테스트
        try {
            if (isNotBlank(settings.getDynamoAccessKey()) && isNotBlank(settings.getDynamoSecretKey())) {
                DynamoDbClient testClient = buildDynamoDbClient(
                        settings.getDynamoRegion(), settings.getDynamoAccessKey(),
                        settings.getDynamoSecretKey(), settings.getEndpoint());
                testClient.listTables(r -> r.limit(1));
                testClient.close();
                result.setDynamoConnected(true);
                result.setDynamoMessage("DynamoDB 연결 성공");
            } else {
                result.setDynamoConnected(false);
                result.setDynamoMessage("Access Key 또는 Secret Key가 비어 있습니다.");
            }
        } catch (Exception e) {
            result.setDynamoConnected(false);
            result.setDynamoMessage("DynamoDB 연결 실패: " + e.getMessage());
            log.warn("SettingsService - DynamoDB 연결 테스트 실패.", e);
        }

        return result;
    }

    /**
     * 현재 유효한 AWS 설정 값을 반환합니다 (DB 우선, 환경변수 fallback).
     */
    public AwsSettingsDTO getEffectiveSettings() {
        Map<String, SystemConfigEntity> dbConfigs = getDbConfigMap();

        AwsSettingsDTO dto = new AwsSettingsDTO();
        dto.setSesRegion(resolveValue(dbConfigs, "aws.ses.region", envSesRegion));
        dto.setSesAccessKey(resolveValue(dbConfigs, "aws.ses.access-key", envSesAccessKey));
        dto.setSesSecretKey(resolveValue(dbConfigs, "aws.ses.secret-key", envSesSecretKey));
        dto.setDynamoRegion(resolveValue(dbConfigs, "aws.dynamo.region", envDynamoRegion));
        dto.setDynamoAccessKey(resolveValue(dbConfigs, "aws.dynamo.access-key", envDynamoAccessKey));
        dto.setDynamoSecretKey(resolveValue(dbConfigs, "aws.dynamo.secret-key", envDynamoSecretKey));
        dto.setEndpoint(resolveValue(dbConfigs, "aws.endpoint", envEndpoint));
        return dto;
    }

    // --- Helper methods ---

    private SesV2Client buildSesV2Client(String region, String accessKey, String secretKey, String endpoint) {
        var builder = SesV2Client.builder()
                .region(Region.of(isNotBlank(region) ? region : "ap-northeast-2"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (isNotBlank(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private DynamoDbClient buildDynamoDbClient(String region, String accessKey, String secretKey, String endpoint) {
        var builder = DynamoDbClient.builder()
                .region(Region.of(isNotBlank(region) ? region : "ap-northeast-2"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (isNotBlank(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private Map<String, SystemConfigEntity> getDbConfigMap() {
        List<SystemConfigEntity> configs = systemConfigRepository.findByKeyPrefix("aws.");
        return configs.stream()
                .collect(Collectors.toMap(SystemConfigEntity::getConfigKey, e -> e));
    }

    private String resolveValue(Map<String, SystemConfigEntity> dbConfigs, String key, String envFallback) {
        SystemConfigEntity dbConfig = dbConfigs.get(key);
        if (dbConfig != null && isNotBlank(dbConfig.getConfigValue())) {
            return dbConfig.getConfigValue();
        }
        return envFallback != null ? envFallback : "";
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
        if (secret == null || secret.length() <= 4) {
            return secret == null || secret.isEmpty() ? "" : "****";
        }
        return secret.substring(0, 4) + "****";
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private boolean hasDbValues(Map<String, SystemConfigEntity> dbConfigs) {
        return dbConfigs.values().stream()
                .anyMatch(e -> isNotBlank(e.getConfigValue()));
    }

    private LocalDateTime getLatestUpdatedAt(Map<String, SystemConfigEntity> dbConfigs) {
        return dbConfigs.values().stream()
                .map(SystemConfigEntity::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}
