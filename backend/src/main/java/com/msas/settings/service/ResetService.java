package com.msas.settings.service;

import com.msas.settings.dto.ResetResultDTO;
import com.msas.settings.repository.ResetRepository;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResetService {

    private final ResetRepository resetRepository;
    private final TenantRepository tenantRepository;
    private final Scheduler scheduler;
    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    /**
     * 발송 결과만 초기화.
     */
    @Transactional
    public ResetResultDTO resetEmailResults() {
        Map<String, Integer> deleted = new LinkedHashMap<>();
        deleted.put("emailEvents", resetRepository.countEmailEventLog());
        deleted.put("emailDetails", resetRepository.countEmailSendDetail());
        deleted.put("emailMasters", resetRepository.countEmailSendMaster());

        resetRepository.truncateEmailEventLog();
        resetRepository.truncateEmailAttachFiles();
        resetRepository.truncateEmailSendDetail();
        resetRepository.truncateEmailSendMaster();
        resetRepository.truncateEmailSendBatch();

        // AWS DynamoDB 이벤트 데이터 초기화
        try {
            String jsonBody = gson.toJson(Map.of("action", "CLEAR_EVENTS"));
            apiGatewayClient.post("/tenant-setup", jsonBody);
        } catch (Exception e) {
            log.warn("[ResetService] DynamoDB 이벤트 데이터 초기화 실패.", e);
        }

        log.info("[ResetService] 발송 결과 초기화 완료. (deleted: {})", deleted);
        return new ResetResultDTO(true, deleted, List.of());
    }

    /**
     * 전체 초기화.
     * 순서: Quartz → AWS 리소스 → 이메일 테이블 → 테넌트 테이블
     */
    public ResetResultDTO resetAll() {
        Map<String, Integer> deleted = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        // 1. Quartz 스케줄러 전체 삭제
        try {
            Set<org.quartz.JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
            deleted.put("quartzJobs", jobKeys.size());
            scheduler.clear();
            log.info("[ResetService] Quartz 스케줄러 전체 삭제 완료. (jobs: {})", jobKeys.size());
        } catch (Exception e) {
            log.warn("[ResetService] Quartz 스케줄러 삭제 실패.", e);
            warnings.add("Quartz 스케줄러 삭제 실패: " + e.getMessage());
            deleted.put("quartzJobs", 0);
        }

        // 2. AWS 리소스 정리 (테넌트별)
        List<TenantEntity> tenants = tenantRepository.selectAllTenants();
        int awsCleanupCount = 0;
        for (TenantEntity tenant : tenants) {
            try {
                cleanupAwsResources(tenant.getTenantId(), tenant.getDomain(), tenant.getConfigSetName());
                awsCleanupCount++;
            } catch (Exception e) {
                log.warn("[ResetService] AWS 리소스 정리 실패. (tenantId: {})", tenant.getTenantId(), e);
                warnings.add("AWS 정리 실패 (tenant: " + tenant.getTenantId() + "): " + e.getMessage());
            }
        }
        deleted.put("awsResources", awsCleanupCount);

        // 3. 이메일 테이블 초기화
        deleted.put("emailEvents", resetRepository.countEmailEventLog());
        deleted.put("emailDetails", resetRepository.countEmailSendDetail());
        deleted.put("emailMasters", resetRepository.countEmailSendMaster());
        deleted.put("tenants", resetRepository.countTenantRegistry());

        resetRepository.truncateEmailEventLog();
        resetRepository.truncateEmailAttachFiles();
        resetRepository.truncateEmailSendDetail();
        resetRepository.truncateEmailSendMaster();
        resetRepository.truncateEmailSendBatch();

        // 4. 테넌트 테이블 초기화
        resetRepository.truncateTenantSender();
        resetRepository.truncateTenantRegistry();

        log.info("[ResetService] 전체 초기화 완료. (deleted: {}, warnings: {})", deleted, warnings.size());
        return new ResetResultDTO(true, deleted, warnings);
    }

    private void cleanupAwsResources(String tenantId, String domain, String configSetName) {
        if (configSetName != null && !configSetName.isBlank()) {
            try {
                String jsonBody = gson.toJson(Map.of("tenantId", tenantId, "action", "DELETE_CONFIGSET"));
                apiGatewayClient.post("/tenant-setup", jsonBody);
            } catch (Exception e) {
                log.warn("[ResetService] ConfigSet 삭제 실패. (tenantId: {})", tenantId, e);
            }
        }
        if (domain != null && !domain.isBlank()) {
            try {
                String jsonBody = gson.toJson(Map.of("domain", domain, "action", "DELETE_IDENTITY"));
                apiGatewayClient.post("/tenant-setup", jsonBody);
            } catch (Exception e) {
                log.warn("[ResetService] SES Identity 삭제 실패. (domain: {})", domain, e);
            }
        }
        try {
            String jsonBody = gson.toJson(Map.of("tenantId", tenantId, "action", "DELETE_TENANT_CONFIG"));
            apiGatewayClient.post("/tenant-setup", jsonBody);
        } catch (Exception e) {
            log.warn("[ResetService] DynamoDB 설정 삭제 실패. (tenantId: {})", tenantId, e);
        }
    }
}
