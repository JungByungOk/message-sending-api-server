package com.msas.onboarding;

import com.msas.ses.configset.SESConfigSetService;
import com.msas.ses.identity.DkimRecordsDTO;
import com.msas.ses.identity.SESIdentityService;
import com.msas.tenant.dto.RequestCreateTenantDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import com.msas.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingService {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;

    @Autowired(required = false)
    private SESIdentityService sesIdentityService;

    @Autowired(required = false)
    private SESConfigSetService sesConfigSetService;

    /**
     * 온보딩 시작: 테넌트 생성 + SES 도메인 아이덴티티 등록
     */
    public OnboardingResultDTO startOnboarding(OnboardingStartRequest request) {
        RequestCreateTenantDTO createRequest = new RequestCreateTenantDTO();
        createRequest.setTenantName(request.getTenantName());
        createRequest.setDomain(request.getDomain());

        ResponseTenantDTO tenant = tenantService.createTenant(createRequest);
        log.info("OnboardingService - 테넌트 생성 완료. (tenantId: {})", tenant.getTenantId());

        DkimRecordsDTO dkimRecords = null;
        if (sesIdentityService != null) {
            try {
                dkimRecords = sesIdentityService.createDomainIdentity(request.getDomain());
                log.info("OnboardingService - SES 도메인 아이덴티티 생성 완료. (domain: {})", request.getDomain());
            } catch (Exception e) {
                log.warn("OnboardingService - SES 도메인 아이덴티티 생성 실패 (건너뜀). (domain: {})", request.getDomain(), e);
            }
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
     * DKIM 레코드 조회
     */
    public DkimRecordsDTO getDkimRecords(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        if (sesIdentityService == null) {
            return new DkimRecordsDTO(entity.getDomain(), entity.getVerificationStatus(), List.of());
        }

        return sesIdentityService.getDomainVerificationStatus(entity.getDomain());
    }

    /**
     * 테넌트 활성화: ConfigSet 생성 + 상태 ACTIVE 로 변경
     */
    public ResponseTenantDTO activate(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        if (sesConfigSetService != null) {
            try {
                String configSetName = sesConfigSetService.createConfigSet(tenantId);
                entity.setConfigSetName(configSetName);
                tenantRepository.updateTenant(entity);
                log.info("OnboardingService - ConfigSet 생성 완료. (configSetName: {})", configSetName);
            } catch (Exception e) {
                log.warn("OnboardingService - ConfigSet 생성 실패 (건너뜀). (tenantId: {})", tenantId, e);
            }
        }

        tenantRepository.updateTenantStatus(tenantId, "ACTIVE");
        log.info("OnboardingService - 테넌트 활성화 완료. (tenantId: {})", tenantId);

        return tenantService.getTenant(tenantId);
    }

    private List<OnboardingStepDTO> buildSteps(TenantEntity entity) {
        List<OnboardingStepDTO> steps = new ArrayList<>();

        // Step 1: 테넌트 생성
        steps.add(new OnboardingStepDTO(1, "테넌트 생성", "COMPLETED"));

        // Step 2: 도메인 인증
        String verificationStatus = entity.getVerificationStatus();
        String step2Status = "SUCCESS".equals(verificationStatus) ? "COMPLETED" : "WAITING";
        steps.add(new OnboardingStepDTO(2, "도메인 인증", step2Status));

        // Step 3: ConfigSet 구성
        String step3Status = entity.getConfigSetName() != null ? "COMPLETED" : "PENDING";
        steps.add(new OnboardingStepDTO(3, "ConfigSet 구성", step3Status));

        // Step 4: 테넌트 활성화
        String step4Status = "ACTIVE".equals(entity.getStatus()) ? "COMPLETED" : "PENDING";
        steps.add(new OnboardingStepDTO(4, "테넌트 활성화", step4Status));

        return steps;
    }
}
