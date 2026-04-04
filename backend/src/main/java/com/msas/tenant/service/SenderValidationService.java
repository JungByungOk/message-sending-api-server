package com.msas.tenant.service;

import com.msas.common.tenant.TenantContext;
import com.msas.tenant.entity.TenantSenderEntity;
import com.msas.tenant.repository.TenantSenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SenderValidationService {

    private final TenantSenderRepository tenantSenderRepository;

    /**
     * 현재 테넌트에 등록된 발신자인지 검증합니다.
     * 테넌트 ID가 없거나 발신자가 등록되지 않은 경우 예외를 발생시킵니다.
     */
    public void validateSender(String fromEmail) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("SenderValidationService - 테넌트 컨텍스트 없음. 발신자 검증 건너뜀.");
            return;
        }

        TenantSenderEntity sender = tenantSenderRepository.findByTenantIdAndEmail(tenantId, fromEmail);
        if (sender == null) {
            throw new IllegalArgumentException(
                    "등록되지 않은 발신자 이메일입니다: " + fromEmail + " (테넌트에 발신자를 먼저 등록하세요)");
        }
    }
}
