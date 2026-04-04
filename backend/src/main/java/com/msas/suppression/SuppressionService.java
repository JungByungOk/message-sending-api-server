package com.msas.suppression;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuppressionService {

    private final SuppressionRepository suppressionRepository;

    public List<SuppressionEntity> getSuppressions(String tenantId, int page, int size) {
        int offset = page * size;
        return suppressionRepository.findByTenantId(tenantId, offset, size);
    }

    public int countSuppressions(String tenantId) {
        return suppressionRepository.countByTenantId(tenantId);
    }

    public boolean isSuppressed(String tenantId, String email) {
        return suppressionRepository.findByTenantIdAndEmail(tenantId, email) != null;
    }

    public void addSuppression(SuppressionEntity entity) {
        SuppressionEntity existing = suppressionRepository.findByTenantIdAndEmail(
                entity.getTenantId(), entity.getEmail());
        if (existing != null) {
            log.debug("SuppressionService - 이미 수신 거부 목록에 등록된 이메일. (email: {})", entity.getEmail());
            return;
        }
        entity.setCreatedAt(LocalDateTime.now());
        suppressionRepository.insert(entity);
        log.info("SuppressionService - 수신 거부 목록 추가. (tenantId: {}, email: {})",
                entity.getTenantId(), entity.getEmail());
    }

    public void removeSuppression(String tenantId, String email) {
        suppressionRepository.deleteByTenantIdAndEmail(tenantId, email);
        log.info("SuppressionService - 수신 거부 목록 제거. (tenantId: {}, email: {})", tenantId, email);
    }
}
