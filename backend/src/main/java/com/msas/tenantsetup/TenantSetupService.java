package com.msas.tenantsetup;

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
public class TenantSetupService {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    /**
     * 테넌트 초기 설정 시작: 테넌트 생성 + API Gateway를 통해 SES 도메인 아이덴티티 등록
     */
    public TenantSetupResultDTO startSetup(TenantSetupStartRequest request) {
        // 1. 테넌트 생성
        RequestCreateTenantDTO createRequest = new RequestCreateTenantDTO();
        createRequest.setTenantName(request.getTenantName());
        createRequest.setDomain(request.getDomain());

        ResponseTenantDTO tenant = tenantService.createTenant(createRequest);
        log.info("[TenantSetupService] 테넌트 생성 완료. (tenantId: {}, domain: {})",
                tenant.getTenantId(), request.getDomain());

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

                // Configuration Set 이름 저장
                String configSetName = result.get("configSetName") != null
                        ? result.get("configSetName").toString() : null;
                if (configSetName != null) {
                    TenantEntity entity = tenantRepository.selectTenantById(tenant.getTenantId());
                    if (entity != null) {
                        entity.setConfigSetName(configSetName);
                        tenantRepository.updateTenant(entity);
                    }
                }

                log.info("[TenantSetupService] AWS 테넌트 설정 완료. (domain: {}, configSet: {})",
                        request.getDomain(), configSetName);
            } else {
                log.warn("[TenantSetupService] AWS 테넌트 설정 실패. (HTTP {}, body: {})",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("[TenantSetupService] AWS 테넌트 설정 실패 (건너뜀). API Gateway가 미구성 상태일 수 있습니다.", e);
        }

        return new TenantSetupResultDTO(tenant, dkimRecords);
    }

    /**
     * 설정 상태 조회
     */
    public TenantSetupStatusDTO getStatus(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        List<TenantSetupStepDTO> steps = buildSteps(entity);

        TenantSetupStatusDTO status = new TenantSetupStatusDTO();
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
                    "/tenant-setup?domain=" + entity.getDomain() + "&action=DKIM_STATUS");

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new TypeToken<Map<String, Object>>() {}.getType());
                return parseDkimRecords(result, entity.getDomain());
            }
        } catch (Exception e) {
            log.warn("[TenantSetupService] DKIM 조회 실패. (tenantId: {})", tenantId, e);
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
                log.info("[TenantSetupService] AWS 활성화 완료. (tenantId: {}, configSet: {})",
                        tenantId, configSetName);
            } else {
                log.warn("[TenantSetupService] AWS 활성화 실패. (HTTP {})", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("[TenantSetupService] AWS 활성화 실패 (건너뜀).", e);
        }

        tenantRepository.updateTenantStatus(tenantId, "ACTIVE");
        log.info("[TenantSetupService] 테넌트 활성화 완료. (tenantId: {})", tenantId);

        return tenantService.getTenant(tenantId);
    }

    /**
     * 이메일 개별 인증 요청 (SES CreateEmailIdentity)
     */
    public EmailVerificationStatusDTO verifyEmail(String tenantId, String email) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        try {
            String jsonBody = gson.toJson(Map.of(
                    "tenantId", tenantId,
                    "email", email,
                    "action", "VERIFY_EMAIL"
            ));

            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new TypeToken<Map<String, Object>>() {}.getType());
                String status = result.getOrDefault("verificationStatus", "PENDING").toString();
                log.info("[TenantSetupService] 이메일 인증 요청 완료. (tenantId: {}, email: {})", tenantId, email);
                return new EmailVerificationStatusDTO(email, status);
            } else {
                log.error("[TenantSetupService] 이메일 인증 요청 실패. (HTTP {}, body: {})",
                        response.statusCode(), response.body());
                throw new RuntimeException("이메일 인증 요청에 실패했습니다. (HTTP " + response.statusCode() + ")");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TenantSetupService] 이메일 인증 요청 실패. (tenantId: {}, email: {})", tenantId, email, e);
            throw new RuntimeException("이메일 인증 요청 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이메일 인증 상태 조회
     */
    public EmailVerificationStatusDTO getEmailVerificationStatus(String tenantId, String email) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        try {
            HttpResponse<String> response = apiGatewayClient.get(
                    "/tenant-setup?tenantId=" + tenantId + "&email=" + email + "&action=EMAIL_STATUS");

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new TypeToken<Map<String, Object>>() {}.getType());
                String status = result.getOrDefault("verificationStatus", "PENDING").toString();
                return new EmailVerificationStatusDTO(email, status);
            } else {
                log.error("[TenantSetupService] 이메일 인증 상태 조회 실패. (HTTP {}, tenantId: {}, email: {})",
                        response.statusCode(), tenantId, email);
                throw new RuntimeException("이메일 인증 상태 조회에 실패했습니다. (HTTP " + response.statusCode() + ")");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TenantSetupService] 이메일 인증 상태 조회 실패. (tenantId: {}, email: {})", tenantId, email, e);
            throw new RuntimeException("이메일 인증 상태 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 인증 이메일 재발송
     */
    public EmailVerificationStatusDTO resendVerification(String tenantId, String email) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        try {
            String jsonBody = gson.toJson(Map.of(
                    "tenantId", tenantId,
                    "email", email,
                    "action", "RESEND_VERIFICATION"
            ));

            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new TypeToken<Map<String, Object>>() {}.getType());
                String status = result.getOrDefault("verificationStatus", "PENDING").toString();
                log.info("[TenantSetupService] 인증 이메일 재발송 완료. (tenantId: {}, email: {})", tenantId, email);
                return new EmailVerificationStatusDTO(email, status);
            } else {
                log.error("[TenantSetupService] 인증 이메일 재발송 실패. (HTTP {}, body: {})",
                        response.statusCode(), response.body());
                throw new RuntimeException("인증 이메일 재발송에 실패했습니다. (HTTP " + response.statusCode() + ")");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TenantSetupService] 인증 이메일 재발송 실패. (tenantId: {}, email: {})", tenantId, email, e);
            throw new RuntimeException("인증 이메일 재발송 중 오류가 발생했습니다.", e);
        }
    }

    // --- Private helpers ---

    private List<TenantSetupStepDTO> buildSteps(TenantEntity entity) {
        List<TenantSetupStepDTO> steps = new ArrayList<>();
        steps.add(new TenantSetupStepDTO(1, "테넌트 생성", "COMPLETED"));

        String verificationStatus = entity.getVerificationStatus();
        String step2Status = "SUCCESS".equals(verificationStatus) ? "COMPLETED" : "WAITING";
        steps.add(new TenantSetupStepDTO(2, "도메인 인증", step2Status));

        String step3Status = entity.getConfigSetName() != null ? "COMPLETED" : "PENDING";
        steps.add(new TenantSetupStepDTO(3, "ConfigSet 구성", step3Status));

        String step4Status = "ACTIVE".equals(entity.getStatus()) ? "COMPLETED" : "PENDING";
        steps.add(new TenantSetupStepDTO(4, "테넌트 활성화", step4Status));

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
