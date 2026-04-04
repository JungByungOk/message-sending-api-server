package com.msas.onboarding;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.msas.ses.identity.DkimRecordDTO;
import com.msas.ses.identity.DkimRecordsDTO;
import com.msas.settings.service.ApiGatewayClient;
import com.msas.tenant.dto.RequestCreateTenantDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import com.msas.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingService {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    /**
     * 온보딩 시작: 테넌트 생성 + API Gateway를 통해 SES 도메인 아이덴티티 등록
     */
    public OnboardingResultDTO startOnboarding(OnboardingStartRequest request) {
        // 1. 테넌트 생성
        RequestCreateTenantDTO createRequest = new RequestCreateTenantDTO();
        createRequest.setTenantName(request.getTenantName());
        createRequest.setDomain(request.getDomain());

        ResponseTenantDTO tenant = tenantService.createTenant(createRequest);
        log.info("OnboardingService - 테넌트 생성 완료. (tenantId: {})", tenant.getTenantId());

        // 2. API Gateway를 통해 테넌트 설정 등록 (SES Identity + ConfigSet + DynamoDB)
        DkimRecordsDTO dkimRecords = null;
        try {
            String jsonBody = gson.toJson(Map.of(
                    "tenantId", tenant.getTenantId(),
                    "tenantName", request.getTenantName(),
                    "domain", request.getDomain(),
                    "action", "CREATE"
            ));

            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new TypeToken<Map<String, Object>>() {}.getType());

                dkimRecords = parseDkimRecords(result, request.getDomain());
                log.info("OnboardingService - AWS 테넌트 설정 완료. (domain: {})", request.getDomain());
            } else {
                log.warn("OnboardingService - AWS 테넌트 설정 실패. (HTTP {}, body: {})",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("OnboardingService - AWS 테넌트 설정 실패 (건너뜀). API Gateway가 미구성 상태일 수 있습니다.", e);
        }

        return new OnboardingResultDTO(tenant, dkimRecords);
    }

    /**
     * 온보딩 상태 조회
     */
    public OnboardingStatusDTO getStatus(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        List<OnboardingStepDTO> steps = buildSteps(entity);

        OnboardingStatusDTO status = new OnboardingStatusDTO();
        status.setTenantId(tenantId);
        status.setDomain(entity.getDomain());
        status.setSteps(steps);
        status.setVerificationStatus(entity.getVerificationStatus());
        status.setTenantStatus(entity.getStatus());
        return status;
    }

    /**
     * DKIM 레코드 조회 (API Gateway 경유)
     */
    public DkimRecordsDTO getDkimRecords(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        try {
            HttpResponse<String> response = apiGatewayClient.get(
                    "/tenant-setup?tenantId=" + tenantId + "&action=DKIM_STATUS");

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new TypeToken<Map<String, Object>>() {}.getType());
                return parseDkimRecords(result, entity.getDomain());
            }
        } catch (Exception e) {
            log.warn("OnboardingService - DKIM 조회 실패.", e);
        }

        return new DkimRecordsDTO(entity.getDomain(), entity.getVerificationStatus(), List.of());
    }

    /**
     * 테넌트 활성화: API Gateway를 통해 ConfigSet 확인 + 상태 ACTIVE로 변경
     */
    public ResponseTenantDTO activate(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        try {
            String jsonBody = gson.toJson(Map.of(
                    "tenantId", tenantId,
                    "domain", entity.getDomain() != null ? entity.getDomain() : "",
                    "action", "ACTIVATE"
            ));

            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new TypeToken<Map<String, Object>>() {}.getType());
                String configSetName = result.get("configSetName") != null
                        ? result.get("configSetName").toString() : null;
                if (configSetName != null) {
                    entity.setConfigSetName(configSetName);
                    tenantRepository.updateTenant(entity);
                }
                log.info("OnboardingService - AWS 활성화 완료. (tenantId: {})", tenantId);
            } else {
                log.warn("OnboardingService - AWS 활성화 실패. (HTTP {})", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("OnboardingService - AWS 활성화 실패 (건너뜀).", e);
        }

        tenantRepository.updateTenantStatus(tenantId, "ACTIVE");
        log.info("OnboardingService - 테넌트 활성화 완료. (tenantId: {})", tenantId);

        return tenantService.getTenant(tenantId);
    }

    // --- Private helpers ---

    private List<OnboardingStepDTO> buildSteps(TenantEntity entity) {
        List<OnboardingStepDTO> steps = new ArrayList<>();
        steps.add(new OnboardingStepDTO(1, "테넌트 생성", "COMPLETED"));

        String verificationStatus = entity.getVerificationStatus();
        String step2Status = "SUCCESS".equals(verificationStatus) ? "COMPLETED" : "WAITING";
        steps.add(new OnboardingStepDTO(2, "도메인 인증", step2Status));

        String step3Status = entity.getConfigSetName() != null ? "COMPLETED" : "PENDING";
        steps.add(new OnboardingStepDTO(3, "ConfigSet 구성", step3Status));

        String step4Status = "ACTIVE".equals(entity.getStatus()) ? "COMPLETED" : "PENDING";
        steps.add(new OnboardingStepDTO(4, "테넌트 활성화", step4Status));

        return steps;
    }

    @SuppressWarnings("unchecked")
    private DkimRecordsDTO parseDkimRecords(Map<String, Object> result, String domain) {
        String verificationStatus = result.getOrDefault("verificationStatus", "PENDING").toString();
        List<DkimRecordDTO> records = new ArrayList<>();

        Object dkimList = result.get("dkimRecords");
        if (dkimList instanceof List) {
            for (Object item : (List<?>) dkimList) {
                if (item instanceof Map) {
                    Map<String, String> rec = (Map<String, String>) item;
                    records.add(new DkimRecordDTO(
                            rec.getOrDefault("name", ""),
                            rec.getOrDefault("type", "CNAME"),
                            rec.getOrDefault("value", "")
                    ));
                }
            }
        }

        return new DkimRecordsDTO(domain, verificationStatus, records);
    }
}
