package com.msas.tenant.service;

import com.msas.tenant.dto.QuotaInfoDTO;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuotaService {

    private final TenantRepository tenantRepository;

    /**
     * 테넌트의 일별/월별 할당량 사용 현황을 조회합니다.
     */
    public QuotaInfoDTO getQuotaUsage(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        int dailyUsed = tenantRepository.countEmailsSentToday(tenantId);
        int monthlyUsed = tenantRepository.countEmailsSentThisMonth(tenantId);

        QuotaInfoDTO dto = new QuotaInfoDTO();
        dto.setTenantId(tenantId);
        dto.setDaily(new QuotaInfoDTO.QuotaDetail(
                entity.getQuotaDaily() != null ? entity.getQuotaDaily() : 0, dailyUsed));
        dto.setMonthly(new QuotaInfoDTO.QuotaDetail(
                entity.getQuotaMonthly() != null ? entity.getQuotaMonthly() : 0, monthlyUsed));

        return dto;
    }

    /**
     * 할당량 초과 여부를 확인합니다.
     */
    public boolean checkQuota(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("테넌트를 찾을 수 없습니다: " + tenantId);
        }

        int dailyUsed = tenantRepository.countEmailsSentToday(tenantId);
        int monthlyUsed = tenantRepository.countEmailsSentThisMonth(tenantId);

        int dailyLimit = entity.getQuotaDaily() != null ? entity.getQuotaDaily() : Integer.MAX_VALUE;
        int monthlyLimit = entity.getQuotaMonthly() != null ? entity.getQuotaMonthly() : Integer.MAX_VALUE;

        boolean withinQuota = dailyUsed < dailyLimit && monthlyUsed < monthlyLimit;
        log.debug("QuotaService - 할당량 확인. (tenantId: {}, daily: {}/{}, monthly: {}/{})",
                tenantId, dailyUsed, dailyLimit, monthlyUsed, monthlyLimit);
        return withinQuota;
    }
}
