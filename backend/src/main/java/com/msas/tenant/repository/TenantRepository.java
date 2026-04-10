package com.msas.tenant.repository;

import com.msas.tenant.entity.TenantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantRepository {

    void insertTenant(TenantEntity tenantEntity);

    TenantEntity selectTenantById(String tenantId);

    TenantEntity selectTenantByApiKey(String apiKey);

    TenantEntity selectTenantByDomain(String domain);

    TenantEntity selectTenantByName(String tenantName);

    List<TenantEntity> selectTenantList(
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int selectTenantCount(@Param("status") String status);

    void updateTenant(TenantEntity tenantEntity);

    void updateTenantStatus(
            @Param("tenantId") String tenantId,
            @Param("status") String status
    );

    void updateVerificationStatus(
            @Param("tenantId") String tenantId,
            @Param("verificationStatus") String verificationStatus
    );

    int countEmailsSentToday(@Param("tenantId") String tenantId);

    int countEmailsSentThisMonth(@Param("tenantId") String tenantId);

    void updateApiKey(
            @Param("tenantId") String tenantId,
            @Param("apiKey") String apiKey
    );

    void updateSesTenantName(
            @Param("tenantId") String tenantId,
            @Param("sesTenantName") String sesTenantName
    );

    void deleteTenant(@Param("tenantId") String tenantId);

    List<TenantEntity> selectAllTenants();
}
