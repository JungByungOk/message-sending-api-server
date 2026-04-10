package com.msas.tenantsetup;

import com.msas.ses.identity.DkimRecordsDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tenant Setup", description = "테넌트 초기 설정 API")
@RestController
@RequestMapping("/tenant-setup")
@RequiredArgsConstructor
public class TenantSetupController {

    private final TenantSetupService tenantSetupService;

    @Operation(summary = "테넌트 초기 설정 시작", description = "신규 테넌트를 생성하고 SES 도메인 아이덴티티를 등록합니다.")
    @PostMapping("/start")
    public ResponseEntity<TenantSetupResultDTO> startSetup(@Valid @RequestBody TenantSetupStartRequest request) {
        TenantSetupResultDTO result = tenantSetupService.startSetup(request);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @Operation(summary = "설정 상태 조회", description = "테넌트의 초기 설정 진행 상태를 단계별로 조회합니다.")
    @GetMapping("/{tenantId}/status")
    public ResponseEntity<TenantSetupStatusDTO> getSetupStatus(@PathVariable String tenantId) {
        TenantSetupStatusDTO status = tenantSetupService.getStatus(tenantId);
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "DKIM 레코드 조회", description = "테넌트 도메인의 DKIM 레코드 및 인증 상태를 조회합니다.")
    @GetMapping("/{tenantId}/dkim")
    public ResponseEntity<DkimRecordsDTO> getDkimRecords(@PathVariable String tenantId) {
        DkimRecordsDTO records = tenantSetupService.getDkimRecords(tenantId);
        return ResponseEntity.ok(records);
    }

    @Operation(summary = "테넌트 수동 활성화", description = "테넌트를 수동으로 활성화하고 ConfigSet을 구성합니다.")
    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<ResponseTenantDTO> activate(@PathVariable String tenantId) {
        ResponseTenantDTO tenant = tenantSetupService.activate(tenantId);
        return ResponseEntity.ok(tenant);
    }

    @Operation(summary = "이메일 인증 요청", description = "테넌트의 이메일 주소에 대한 SES 개별 인증을 요청합니다.")
    @PostMapping("/{tenantId}/verify-email")
    public ResponseEntity<EmailVerificationStatusDTO> verifyEmail(
            @PathVariable String tenantId,
            @Valid @RequestBody VerifyEmailRequest request) {
        EmailVerificationStatusDTO result = tenantSetupService.verifyEmail(tenantId, request.getEmail());
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @Operation(summary = "이메일 인증 상태 조회", description = "테넌트의 이메일 인증 상태를 조회합니다.")
    @GetMapping("/{tenantId}/email-status/{email}")
    public ResponseEntity<EmailVerificationStatusDTO> getEmailVerificationStatus(
            @PathVariable String tenantId,
            @PathVariable String email) {
        EmailVerificationStatusDTO status = tenantSetupService.getEmailVerificationStatus(tenantId, email);
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "인증 이메일 재발송", description = "테넌트의 이메일 인증 메일을 재발송합니다.")
    @PostMapping("/{tenantId}/resend-verification/{email}")
    public ResponseEntity<EmailVerificationStatusDTO> resendVerification(
            @PathVariable String tenantId,
            @PathVariable String email) {
        EmailVerificationStatusDTO result = tenantSetupService.resendVerification(tenantId, email);
        return ResponseEntity.ok(result);
    }
}
