package com.msas.onboarding;

import com.msas.ses.identity.DkimRecordsDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Onboarding", description = "테넌트 온보딩 API")
@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "온보딩 시작", description = "신규 테넌트 온보딩을 시작합니다. 테넌트를 생성하고 SES 도메인 아이덴티티를 등록합니다.")
    @PostMapping("/start")
    public ResponseEntity<OnboardingResultDTO> startOnboarding(@Valid @RequestBody OnboardingStartRequest request) {
        OnboardingResultDTO result = onboardingService.startOnboarding(request);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @Operation(summary = "온보딩 상태 조회", description = "테넌트의 온보딩 진행 상태를 단계별로 조회합니다.")
    @GetMapping("/{tenantId}/status")
    public ResponseEntity<OnboardingStatusDTO> getOnboardingStatus(@PathVariable String tenantId) {
        OnboardingStatusDTO status = onboardingService.getStatus(tenantId);
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "DKIM 레코드 조회", description = "테넌트 도메인의 DKIM 레코드 및 인증 상태를 조회합니다.")
    @GetMapping("/{tenantId}/dkim")
    public ResponseEntity<DkimRecordsDTO> getDkimRecords(@PathVariable String tenantId) {
        DkimRecordsDTO records = onboardingService.getDkimRecords(tenantId);
        return ResponseEntity.ok(records);
    }

    @Operation(summary = "테넌트 수동 활성화", description = "테넌트를 수동으로 활성화하고 ConfigSet을 구성합니다.")
    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<ResponseTenantDTO> activate(@PathVariable String tenantId) {
        ResponseTenantDTO tenant = onboardingService.activate(tenantId);
        return ResponseEntity.ok(tenant);
    }
}
