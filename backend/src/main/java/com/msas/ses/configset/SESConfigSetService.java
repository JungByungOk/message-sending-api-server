package com.msas.ses.configset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(SesV2Client.class)
public class SESConfigSetService {

    private static final String CONFIG_SET_PREFIX = "tenant-";

    private final SesV2Client sesV2Client;

    /**
     * 테넌트 ID 기반 ConfigSet을 생성합니다. (이름: tenant-{tenantId})
     */
    public String createConfigSet(String tenantId) {
        String configSetName = CONFIG_SET_PREFIX + tenantId;

        CreateConfigurationSetRequest request = CreateConfigurationSetRequest.builder()
                .configurationSetName(configSetName)
                .build();

        sesV2Client.createConfigurationSet(request);
        log.info("SESConfigSetService - ConfigSet 생성 완료. (configSetName: {})", configSetName);

        return configSetName;
    }

    /**
     * ConfigSet을 삭제합니다.
     */
    public void deleteConfigSet(String configSetName) {
        DeleteConfigurationSetRequest request = DeleteConfigurationSetRequest.builder()
                .configurationSetName(configSetName)
                .build();

        sesV2Client.deleteConfigurationSet(request);
        log.info("SESConfigSetService - ConfigSet 삭제 완료. (configSetName: {})", configSetName);
    }

    /**
     * ConfigSet 정보를 조회합니다.
     */
    public GetConfigurationSetResponse getConfigSet(String tenantId) {
        String configSetName = CONFIG_SET_PREFIX + tenantId;

        GetConfigurationSetRequest request = GetConfigurationSetRequest.builder()
                .configurationSetName(configSetName)
                .build();

        return sesV2Client.getConfigurationSet(request);
    }
}
