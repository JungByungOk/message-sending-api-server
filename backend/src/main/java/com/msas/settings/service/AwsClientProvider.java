package com.msas.settings.service;

import com.msas.settings.dto.AwsSettingsDTO;
import com.msas.settings.entity.SystemConfigEntity;
import com.msas.settings.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AWS 클라이언트를 동적으로 관리하는 Provider.
 * DB 설정이 있으면 우선 사용하고, 없으면 환경변수로 fallback합니다.
 * 설정 변경 시 refresh()를 호출하면 클라이언트를 재생성합니다.
 */
@Component
@Slf4j
public class AwsClientProvider {

    private final SystemConfigRepository systemConfigRepository;

    private final AtomicReference<SesClient> sesClientRef = new AtomicReference<>();
    private final AtomicReference<SesV2Client> sesV2ClientRef = new AtomicReference<>();
    private final AtomicReference<DynamoDbClient> dynamoDbClientRef = new AtomicReference<>();

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

    private volatile AwsSettingsDTO overrideSettings;

    public AwsClientProvider(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @PostConstruct
    public void init() {
        // 앱 시작 시 DB에서 설정을 읽어 초기화
        loadDbSettings();
        buildClients();
    }

    private void loadDbSettings() {
        try {
            var configs = systemConfigRepository.findByKeyPrefix("aws.");
            if (configs == null || configs.isEmpty()) return;

            var map = new java.util.HashMap<String, String>();
            for (SystemConfigEntity e : configs) {
                if (e.getConfigValue() != null && !e.getConfigValue().isBlank()) {
                    map.put(e.getConfigKey(), e.getConfigValue());
                }
            }
            if (map.isEmpty()) return;

            AwsSettingsDTO dto = new AwsSettingsDTO();
            dto.setSesRegion(map.getOrDefault("aws.ses.region", ""));
            dto.setSesAccessKey(map.getOrDefault("aws.ses.access-key", ""));
            dto.setSesSecretKey(map.getOrDefault("aws.ses.secret-key", ""));
            dto.setDynamoRegion(map.getOrDefault("aws.dynamo.region", ""));
            dto.setDynamoAccessKey(map.getOrDefault("aws.dynamo.access-key", ""));
            dto.setDynamoSecretKey(map.getOrDefault("aws.dynamo.secret-key", ""));
            dto.setEndpoint(map.getOrDefault("aws.endpoint", ""));
            this.overrideSettings = dto;
            log.info("AwsClientProvider - DB에서 AWS 설정 로드 완료.");
        } catch (Exception e) {
            log.warn("AwsClientProvider - DB 설정 로드 실패 (환경변수 사용).", e);
        }
    }

    /**
     * DB에서 읽은 설정으로 클라이언트를 재생성합니다.
     */
    public void refresh(AwsSettingsDTO settings) {
        this.overrideSettings = settings;
        buildClients();
        log.info("AwsClientProvider - AWS 클라이언트 재생성 완료.");
    }

    public SesClient getSesClient() {
        return sesClientRef.get();
    }

    public SesV2Client getSesV2Client() {
        return sesV2ClientRef.get();
    }

    public DynamoDbClient getDynamoDbClient() {
        return dynamoDbClientRef.get();
    }

    public boolean isSesAvailable() {
        return sesClientRef.get() != null;
    }

    public boolean isSesV2Available() {
        return sesV2ClientRef.get() != null;
    }

    public boolean isDynamoAvailable() {
        return dynamoDbClientRef.get() != null;
    }

    private void buildClients() {
        String sesRegion = resolve(overrideSettings != null ? overrideSettings.getSesRegion() : null, envSesRegion);
        String sesAccessKey = resolve(overrideSettings != null ? overrideSettings.getSesAccessKey() : null, envSesAccessKey);
        String sesSecretKey = resolve(overrideSettings != null ? overrideSettings.getSesSecretKey() : null, envSesSecretKey);
        String dynamoRegion = resolve(overrideSettings != null ? overrideSettings.getDynamoRegion() : null, envDynamoRegion);
        String dynamoAccessKey = resolve(overrideSettings != null ? overrideSettings.getDynamoAccessKey() : null, envDynamoAccessKey);
        String dynamoSecretKey = resolve(overrideSettings != null ? overrideSettings.getDynamoSecretKey() : null, envDynamoSecretKey);
        String endpoint = resolve(overrideSettings != null ? overrideSettings.getEndpoint() : null, envEndpoint);

        // SES v1
        closeSilently(sesClientRef.get());
        if (isNotBlank(sesAccessKey) && isNotBlank(sesSecretKey)) {
            try {
                sesClientRef.set(buildSesClient(sesRegion, sesAccessKey, sesSecretKey, endpoint));
            } catch (Exception e) {
                log.warn("AwsClientProvider - SES v1 클라이언트 생성 실패.", e);
                sesClientRef.set(null);
            }
        } else {
            sesClientRef.set(null);
        }

        // SES v2
        closeSilently(sesV2ClientRef.get());
        if (isNotBlank(sesAccessKey) && isNotBlank(sesSecretKey)) {
            try {
                sesV2ClientRef.set(buildSesV2Client(sesRegion, sesAccessKey, sesSecretKey, endpoint));
            } catch (Exception e) {
                log.warn("AwsClientProvider - SES v2 클라이언트 생성 실패.", e);
                sesV2ClientRef.set(null);
            }
        } else {
            sesV2ClientRef.set(null);
        }

        // DynamoDB
        closeSilently(dynamoDbClientRef.get());
        if (isNotBlank(dynamoAccessKey) && isNotBlank(dynamoSecretKey)) {
            try {
                dynamoDbClientRef.set(buildDynamoDbClient(dynamoRegion, dynamoAccessKey, dynamoSecretKey, endpoint));
            } catch (Exception e) {
                log.warn("AwsClientProvider - DynamoDB 클라이언트 생성 실패.", e);
                dynamoDbClientRef.set(null);
            }
        } else {
            dynamoDbClientRef.set(null);
        }
    }

    private SesClient buildSesClient(String region, String accessKey, String secretKey, String endpoint) {
        var builder = SesClient.builder()
                .region(Region.of(isNotBlank(region) ? region : "ap-northeast-2"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (isNotBlank(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

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

    private String resolve(String dbValue, String envValue) {
        if (isNotBlank(dbValue)) return dbValue;
        return envValue != null ? envValue : "";
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private void closeSilently(AutoCloseable client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}
