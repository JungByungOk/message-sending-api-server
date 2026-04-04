package com.msas.tenant.controller;

import com.msas.tenant.dto.QuotaInfoDTO;
import com.msas.tenant.dto.RequestCreateTenantDTO;
import com.msas.tenant.dto.RequestUpdateTenantDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import com.msas.tenant.dto.ResponseTenantListDTO;
import com.msas.tenant.entity.TenantSenderEntity;
import com.msas.tenant.repository.TenantSenderRepository;
import com.msas.tenant.service.QuotaService;
import com.msas.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Tenant", description = "테넌트 관리 API")
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final QuotaService quotaService;
    private final TenantSenderRepository tenantSenderRepository;

    @Operation(summary = "테넌트 생성", description = "신규 테넌트를 등록합니다.")
    @PostMapping
    public ResponseEntity<ResponseTenantDTO> createTenant(@Valid @RequestBody RequestCreateTenantDTO request) {
        ResponseTenantDTO response = tenantService.createTenant(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "테넌트 조회", description = "테넌트 ID로 테넌트 정보를 조회합니다.")
    @GetMapping("/{tenantId}")
    public ResponseEntity<ResponseTenantDTO> getTenant(@PathVariable String tenantId) {
        ResponseTenantDTO response = tenantService.getTenant(tenantId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "테넌트 목록 조회", description = "상태별 테넌트 목록을 페이징 조회합니다.")
    @GetMapping("/list")
    public ResponseEntity<ResponseTenantListDTO> getTenants(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ResponseTenantListDTO response = tenantService.getTenants(status, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "테넌트 수정", description = "테넌트 이름 및 할당량을 수정합니다.")
    @PatchMapping("/{tenantId}")
    public ResponseEntity<ResponseTenantDTO> updateTenant(
            @PathVariable String tenantId,
            @RequestBody RequestUpdateTenantDTO request) {
        ResponseTenantDTO response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "테넌트 비활성화", description = "테넌트 상태를 INACTIVE로 변경합니다.")
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deactivateTenant(@PathVariable String tenantId) {
        tenantService.deactivateTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "테넌트 영구 삭제", description = "비활성 상태의 테넌트를 영구 삭제합니다.")
    @DeleteMapping("/{tenantId}/permanent")
    public ResponseEntity<Void> deleteTenant(@PathVariable String tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "테넌트 활성화", description = "비활성 상태의 테넌트를 다시 활성화합니다.")
    @PostMapping("/{tenantId}/activate")
    public ResponseEntity<Void> activateTenant(@PathVariable String tenantId) {
        tenantService.activateTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "API 키 재발급", description = "테넌트의 API 키를 새로 발급합니다.")
    @PostMapping("/{tenantId}/regenerate-key")
    public ResponseEntity<ResponseTenantDTO> regenerateApiKey(@PathVariable String tenantId) {
        ResponseTenantDTO response = tenantService.regenerateApiKey(tenantId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "할당량 사용 현황 조회", description = "테넌트의 일별/월별 이메일 할당량 사용 현황을 조회합니다.")
    @GetMapping("/{tenantId}/quota")
    public ResponseEntity<QuotaInfoDTO> getQuotaUsage(@PathVariable String tenantId) {
        QuotaInfoDTO quota = quotaService.getQuotaUsage(tenantId);
        return ResponseEntity.ok(quota);
    }

    @Operation(summary = "할당량 수정", description = "테넌트의 일별/월별 이메일 할당량을 수정합니다.")
    @PatchMapping("/{tenantId}/quota")
    public ResponseEntity<ResponseTenantDTO> updateQuota(
            @PathVariable String tenantId,
            @RequestBody RequestUpdateTenantDTO request) {
        ResponseTenantDTO response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(response);
    }

    // === 발신자 이메일 관리 ===

    @Operation(summary = "발신자 목록 조회", description = "테넌트에 등록된 발신자 이메일 목록을 조회합니다.")
    @GetMapping("/{tenantId}/senders")
    public ResponseEntity<List<TenantSenderEntity>> getSenders(@PathVariable String tenantId) {
        return ResponseEntity.ok(tenantSenderRepository.findByTenantId(tenantId));
    }

    @Operation(summary = "발신자 등록", description = "테넌트의 인증된 도메인 기반 발신자 이메일을 등록합니다.")
    @PostMapping("/{tenantId}/senders")
    public ResponseEntity<TenantSenderEntity> addSender(
            @PathVariable String tenantId,
            @RequestBody TenantSenderEntity sender) {
        // 도메인 검증: 테넌트 도메인과 일치하는지 확인
        ResponseTenantDTO tenant = tenantService.getTenant(tenantId);
        String domain = tenant.getDomain();
        if (domain != null && !sender.getEmail().endsWith("@" + domain)) {
            throw new IllegalArgumentException(
                    "발신자 이메일은 테넌트 도메인(@" + domain + ")만 허용됩니다.");
        }
        sender.setTenantId(tenantId);
        tenantSenderRepository.insert(sender);
        return new ResponseEntity<>(sender, HttpStatus.CREATED);
    }

    @Operation(summary = "발신자 삭제", description = "테넌트의 발신자 이메일을 삭제합니다.")
    @DeleteMapping("/{tenantId}/senders/{email}")
    public ResponseEntity<Void> removeSender(
            @PathVariable String tenantId,
            @PathVariable String email) {
        tenantSenderRepository.deleteByTenantIdAndEmail(tenantId, email);
        return ResponseEntity.noContent().build();
    }
}
