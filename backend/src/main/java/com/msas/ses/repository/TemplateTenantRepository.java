package com.msas.ses.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TemplateTenantRepository {

    int insertMapping(@Param("templateName") String templateName, @Param("tenantId") String tenantId, @Param("subject") String subject);

    int deleteMapping(@Param("templateName") String templateName, @Param("tenantId") String tenantId);

    int deleteMappingsByTemplate(@Param("templateName") String templateName);

    List<String> selectTenantsByTemplate(@Param("templateName") String templateName);

    List<String> selectTemplatesByTenant(@Param("tenantId") String tenantId);

    List<Map<String, Object>> selectAllMappings();

    String selectSubjectByTemplate(@Param("templateName") String templateName);

    int updateSubjectByTemplate(@Param("templateName") String templateName, @Param("subject") String subject);
}
