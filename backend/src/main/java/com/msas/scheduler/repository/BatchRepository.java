package com.msas.scheduler.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface BatchRepository {

    int insertBatch(@Param("batch_id") String batchId,
                    @Param("template_name") String templateName,
                    @Param("from_addr") String fromAddr,
                    @Param("tags") String tags,
                    @Param("job_name") String jobName,
                    @Param("job_group") String jobGroup,
                    @Param("description") String description,
                    @Param("start_date_at") LocalDateTime startDateAt,
                    @Param("total_count") int totalCount,
                    @Param("tenant_id") String tenantId);

    int insertBatchItem(@Param("batch_id") String batchId,
                        @Param("item_index") int itemIndex,
                        @Param("correlation_id") String correlationId,
                        @Param("email_send_dtl_seq") Integer emailSendDtlSeq,
                        @Param("to_addrs") String toAddrs,
                        @Param("cc_addrs") String ccAddrs,
                        @Param("bcc_addrs") String bccAddrs,
                        @Param("template_parameters") String templateParameters);

    int insertBatchItems(@Param("items") List<Map<String, Object>> items);

    Map<String, Object> selectBatch(@Param("batch_id") String batchId);

    List<Map<String, Object>> selectBatchItems(@Param("batch_id") String batchId);

    int updateBatchStatus(@Param("batch_id") String batchId,
                          @Param("status") String status);

    int deleteBatch(@Param("batch_id") String batchId);
}
