package com.msas.monitoring;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MonitoringRepository {

    Map<String, Object> selectTodayEmailStats();

    Map<String, Object> selectBatchStats();

    List<Map<String, Object>> selectHourlyStats(@Param("targetDate") String targetDate);

    List<Map<String, Object>> selectStatusSummary();

    List<Map<String, Object>> selectBounceList(@Param("offset") int offset, @Param("size") int size);

    long countBounceList();

    List<Map<String, Object>> selectBatchList();

    Map<String, Object> selectRecentBatch();

    Map<String, Object> selectPrevBatch(@Param("batchId") String batchId);

    Map<String, Object> selectBatchDeliveryStats(@Param("batchId") String batchId);

    List<Map<String, Object>> selectBatchEventStats(@Param("batchId") String batchId);

    List<Map<String, Object>> selectWeeklyTrend(@Param("count") int count);

    List<Map<String, Object>> selectMonthlyTrend(@Param("count") int count);

    List<Map<String, Object>> selectMonthlySendCounts(@Param("months") int months);

    List<Map<String, Object>> selectMonthlyEventCounts(@Param("months") int months);

    List<Map<String, Object>> selectTenantDailySummary();

    Map<String, Object> selectTenantConfigSetName(@Param("tenantId") String tenantId);
}
