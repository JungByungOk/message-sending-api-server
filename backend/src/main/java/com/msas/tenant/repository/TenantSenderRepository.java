package com.msas.tenant.repository;

import com.msas.tenant.entity.TenantSenderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantSenderRepository {

    List<TenantSenderEntity> findByTenantId(@Param("tenantId") String tenantId);

    TenantSenderEntity findByTenantIdAndEmail(@Param("tenantId") String tenantId, @Param("email") String email);

    TenantSenderEntity findDefaultByTenantId(@Param("tenantId") String tenantId);

    void insert(TenantSenderEntity entity);

    void deleteByTenantIdAndEmail(@Param("tenantId") String tenantId, @Param("email") String email);
}
