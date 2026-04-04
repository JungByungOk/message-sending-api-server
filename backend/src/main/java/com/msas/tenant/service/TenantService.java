package com.msas.tenant.service;

import com.msas.tenant.dto.RequestCreateTenantDTO;
import com.msas.tenant.dto.RequestUpdateTenantDTO;
import com.msas.tenant.dto.ResponseTenantDTO;
import com.msas.tenant.dto.ResponseTenantListDTO;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {

    private static final int API_KEY_BYTES = 24;
    private static final String API_KEY_PREFIX = "sk-";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String VERIFICATION_PENDING = "PENDING";

    private final TenantRepository tenantRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ResponseTenantDTO createTenant(RequestCreateTenantDTO request) {
        TenantEntity existingName = tenantRepository.selectTenantByName(request.getTenantName());
        if (existingName != null) {
            throw new IllegalArgumentException("DUPLICATE_TENANT_NAME:" + request.getTenantName());
        }

        TenantEntity existing = tenantRepository.selectTenantByDomain(request.getDomain());
        if (existing != null) {
            throw new IllegalArgumentException("DUPLICATE_DOMAIN:" + request.getDomain());
        }

        LocalDateTime now = LocalDateTime.now();

        TenantEntity entity = new TenantEntity();
        entity.setTenantId("ems-" + UUID.randomUUID().toString());
        entity.setTenantName(request.getTenantName());
        entity.setDomain(request.getDomain());
        entity.setApiKey(generateApiKey());
        entity.setVerificationStatus(VERIFICATION_PENDING);
        entity.setQuotaDaily(request.getQuotaDaily() != null ? request.getQuotaDaily() : 10000);
        entity.setQuotaMonthly(request.getQuotaMonthly() != null ? request.getQuotaMonthly() : 300000);
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        tenantRepository.insertTenant(entity);
        log.info("TenantService - Tenant created. (tenantId: {})", entity.getTenantId());

        return toResponseDTO(entity);
    }

    public ResponseTenantDTO getTenant(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        return toResponseDTO(entity);
    }

    public ResponseTenantListDTO getTenants(String status, int page, int size) {
        int offset = page * size;
        List<TenantEntity> entityList = tenantRepository.selectTenantList(status, offset, size);
        int totalCount = tenantRepository.selectTenantCount(status);

        List<ResponseTenantDTO> tenants = entityList.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        ResponseTenantListDTO response = new ResponseTenantListDTO();
        response.setTotalCount(totalCount);
        response.setTenants(tenants);
        return response;
    }

    public ResponseTenantDTO updateTenant(String tenantId, RequestUpdateTenantDTO request) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }

        if (request.getTenantName() != null) {
            entity.setTenantName(request.getTenantName());
        }
        if (request.getQuotaDaily() != null) {
            entity.setQuotaDaily(request.getQuotaDaily());
        }
        if (request.getQuotaMonthly() != null) {
            entity.setQuotaMonthly(request.getQuotaMonthly());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        tenantRepository.updateTenant(entity);
        log.info("TenantService - Tenant updated. (tenantId: {})", tenantId);

        return toResponseDTO(entity);
    }

    public void deactivateTenant(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        tenantRepository.updateTenantStatus(tenantId, STATUS_INACTIVE);
        log.info("TenantService - Tenant deactivated. (tenantId: {})", tenantId);
    }

    public void activateTenant(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        if (!STATUS_INACTIVE.equals(entity.getStatus())) {
            throw new IllegalArgumentException("Only inactive tenants can be activated. Current status: " + entity.getStatus());
        }
        tenantRepository.updateTenantStatus(tenantId, STATUS_ACTIVE);
        log.info("TenantService - Tenant activated. (tenantId: {})", tenantId);
    }

    public ResponseTenantDTO regenerateApiKey(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }

        String newApiKey = generateApiKey();
        entity.setApiKey(newApiKey);
        entity.setUpdatedAt(LocalDateTime.now());

        tenantRepository.updateApiKey(tenantId, newApiKey);
        log.info("TenantService - API key regenerated. (tenantId: {})", tenantId);

        return toResponseDTO(entity);
    }

    public void deleteTenant(String tenantId) {
        TenantEntity entity = tenantRepository.selectTenantById(tenantId);
        if (entity == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        if (!STATUS_INACTIVE.equals(entity.getStatus())) {
            throw new IllegalArgumentException("Only inactive tenants can be deleted. Current status: " + entity.getStatus());
        }
        tenantRepository.deleteTenant(tenantId);
        log.info("TenantService - Tenant permanently deleted. (tenantId: {})", tenantId);
    }

    public TenantEntity getTenantByApiKey(String apiKey) {
        return tenantRepository.selectTenantByApiKey(apiKey);
    }

    private String generateApiKey() {
        byte[] bytes = new byte[API_KEY_BYTES];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(API_KEY_PREFIX);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private ResponseTenantDTO toResponseDTO(TenantEntity entity) {
        ResponseTenantDTO dto = new ResponseTenantDTO();
        dto.setTenantId(entity.getTenantId());
        dto.setTenantName(entity.getTenantName());
        dto.setDomain(entity.getDomain());
        dto.setApiKey(entity.getApiKey());
        dto.setConfigSetName(entity.getConfigSetName());
        dto.setVerificationStatus(entity.getVerificationStatus());
        dto.setQuotaDaily(entity.getQuotaDaily());
        dto.setQuotaMonthly(entity.getQuotaMonthly());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
