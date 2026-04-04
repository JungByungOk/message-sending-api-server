package com.msas.settings.repository;

import com.msas.settings.entity.SystemConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemConfigRepository {

    List<SystemConfigEntity> findAll();

    List<SystemConfigEntity> findByKeyPrefix(@Param("prefix") String prefix);

    SystemConfigEntity findByKey(@Param("configKey") String configKey);

    void upsert(SystemConfigEntity entity);
}
