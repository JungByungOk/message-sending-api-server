package com.msas.suppression;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SuppressionRepository {

    List<SuppressionEntity> findByTenantId(
            @Param("tenantId") String tenantId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    SuppressionEntity findByTenantIdAndEmail(
            @Param("tenantId") String tenantId,
            @Param("email") String email
    );

    int countByTenantId(@Param("tenantId") String tenantId);

    void insert(SuppressionEntity entity);

    void deleteByTenantIdAndEmail(
            @Param("tenantId") String tenantId,
            @Param("email") String email
    );
}
