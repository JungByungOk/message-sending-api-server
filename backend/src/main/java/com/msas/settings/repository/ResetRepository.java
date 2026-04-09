package com.msas.settings.repository;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResetRepository {
    void truncateEmailEventLog();
    void truncateEmailAttachFiles();
    void truncateEmailSendDetail();
    void truncateEmailSendMaster();
    void truncateEmailSendBatch();
    void truncateTenantSender();
    void truncateTenantRegistry();
    void truncateTemplateTenantMap();

    int countEmailSendMaster();
    int countEmailSendDetail();
    int countEmailEventLog();
    int countTenantRegistry();
}
