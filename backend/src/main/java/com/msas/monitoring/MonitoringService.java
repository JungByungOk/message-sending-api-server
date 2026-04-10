package com.msas.monitoring;

import com.google.gson.Gson;
import com.msas.settings.service.ApiGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final MonitoringRepository monitoringRepository;
    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    public MonitoringSummaryDTO getSummary() {
        Map<String, Object> emailStats = monitoringRepository.selectTodayEmailStats();
        Map<String, Object> batchStats = monitoringRepository.selectBatchStats();

        if (emailStats == null) emailStats = new HashMap<>();
        if (batchStats == null) batchStats = new HashMap<>();

        long sentCount = toLong(emailStats.get("todaysentcount"));
        long deliveredCount = toLong(emailStats.get("todaydeliveredcount"));
        long bounceCount = toLong(emailStats.get("todaybouncecount"));
        long complaintCount = toLong(emailStats.get("todaycomplaintcount"));

        MonitoringSummaryDTO dto = new MonitoringSummaryDTO();
        dto.setTodaySentCount(sentCount);
        dto.setTodayDeliveredCount(deliveredCount);
        dto.setTodayBounceCount(bounceCount);
        dto.setTodayComplaintCount(complaintCount);
        dto.setDeliveryRate(rate(deliveredCount, sentCount));
        dto.setBounceRate(rate(bounceCount, sentCount));
        dto.setRunningBatchCount(toLong(batchStats.get("runningbatchcount")));
        dto.setPendingBatchCount(toLong(batchStats.get("pendingbatchcount")));

        return dto;
    }

    public List<Map<String, Object>> getHourlyStats(String date) {
        String targetDate = (date != null && !date.isBlank()) ? date : LocalDate.now().toString();
        List<Map<String, Object>> result = monitoringRepository.selectHourlyStats(targetDate);
        Map<Integer, Long> hourMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            int hour = toInt(row.get("hour"));
            hourMap.put(hour, toLong(row.get("sentcount")));
        }
        List<Map<String, Object>> full = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("hour", h);
            slot.put("sentCount", hourMap.getOrDefault(h, 0L));
            full.add(slot);
        }
        return full;
    }

    public List<Map<String, Object>> getStatusSummary() {
        return monitoringRepository.selectStatusSummary();
    }

    public Map<String, Object> getBounceList(int page, int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = monitoringRepository.selectBounceList(offset, size);
        long total = monitoringRepository.countBounceList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", toLong(r.get("id")));
            item.put("email", r.get("email"));
            item.put("fromEmail", r.get("fromemail"));
            item.put("subject", r.get("subject"));
            item.put("status", r.get("status"));
            item.put("regDtm", r.get("regdtm") != null ? r.get("regdtm").toString() : null);
            item.put("tenantId", r.get("tenantid"));
            list.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public List<Map<String, Object>> getBatchList() {
        List<Map<String, Object>> rows = monitoringRepository.selectBatchList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("batchId", r.get("batchid"));
            item.put("templateName", r.get("templatename"));
            item.put("fromAddr", r.get("fromaddr"));
            item.put("jobName", r.get("jobname"));
            item.put("status", r.get("status"));
            item.put("totalCount", toLong(r.get("totalcount")));
            item.put("startDateAt", r.get("startdateat") != null ? r.get("startdateat").toString() : null);
            result.add(item);
        }
        return result;
    }

    public RecentCampaignDTO getRecentCampaign() {
        Map<String, Object> batch = monitoringRepository.selectRecentBatch();
        if (batch == null) return new RecentCampaignDTO();

        String batchId = (String) batch.get("batchid");
        String status = (String) batch.get("status");
        boolean isComplete = "COMPLETED".equals(status) || "COMPLETED_WITH_ERRORS".equals(status);

        Map<String, Object> delivery = monitoringRepository.selectBatchDeliveryStats(batchId);
        if (delivery == null) delivery = new HashMap<>();
        long totalSent = toLong(delivery.get("totalsent"));
        long delivered = toLong(delivery.get("delivered"));

        List<Map<String, Object>> events = monitoringRepository.selectBatchEventStats(batchId);
        long opens = 0, clicks = 0, complaints = 0;
        for (Map<String, Object> e : events) {
            String type = (String) e.get("eventtype");
            long cnt = toLong(e.get("cnt"));
            if ("Open".equals(type))      opens = cnt;
            else if ("Click".equals(type)) clicks = cnt;
            else if ("Complaint".equals(type)) complaints = cnt;
        }

        RecentCampaignDTO dto = new RecentCampaignDTO();
        dto.setBatchId(batchId);
        dto.setTemplateName((String) batch.get("templatename"));
        dto.setStartDateAt(batch.get("startdateat") != null ? batch.get("startdateat").toString() : null);
        dto.setStatus(status);
        dto.setComplete(isComplete);
        dto.setTotalSent(totalSent);
        dto.setDelivered(delivered);
        dto.setDeliveryRate(rate(delivered, totalSent));
        dto.setOpens(opens);
        dto.setOpenRate(rate(opens, delivered));
        dto.setClicks(clicks);
        dto.setClickRate(rate(clicks, delivered));
        dto.setComplaints(complaints);
        dto.setComplaintRate(rate(complaints, totalSent));

        Map<String, Object> prevBatch = monitoringRepository.selectPrevBatch(batchId);
        if (prevBatch != null) {
            String prevBatchId = (String) prevBatch.get("batchid");
            long prevTotal = toLong(prevBatch.get("totalcount"));
            if (prevTotal > 0) {
                Map<String, Object> prevDelivery = monitoringRepository.selectBatchDeliveryStats(prevBatchId);
                if (prevDelivery == null) prevDelivery = new HashMap<>();
                long prevTotalSent = toLong(prevDelivery.get("totalsent"));
                long prevDelivered = toLong(prevDelivery.get("delivered"));
                List<Map<String, Object>> prevEvents = monitoringRepository.selectBatchEventStats(prevBatchId);
                long prevOpens = 0, prevClicks = 0, prevComplaints = 0;
                for (Map<String, Object> e : prevEvents) {
                    String type = (String) e.get("eventtype");
                    long cnt = toLong(e.get("cnt"));
                    if ("Open".equals(type))           prevOpens = cnt;
                    else if ("Click".equals(type))     prevClicks = cnt;
                    else if ("Complaint".equals(type)) prevComplaints = cnt;
                }
                dto.setPrevDeliveryRate(rate(prevDelivered, prevTotalSent));
                dto.setPrevOpenRate(rate(prevOpens, prevDelivered));
                dto.setPrevClickRate(rate(prevClicks, prevDelivered));
                dto.setPrevComplaintRate(rate(prevComplaints, prevTotalSent));
            }
        }
        return dto;
    }

    public List<Map<String, Object>> getTrend(String period, int count) {
        int safeCount = Math.min(Math.max(count, 1), 24);
        List<Map<String, Object>> rows;
        if ("monthly".equalsIgnoreCase(period)) {
            rows = monitoringRepository.selectMonthlyTrend(safeCount);
        } else {
            rows = monitoringRepository.selectWeeklyTrend(safeCount);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long deliveredVal = toLong(r.get("deliveredcount"));
            long sent = toLong(r.get("sentcount"));
            long opensVal = toLong(r.get("opencount"));
            long clicksVal = toLong(r.get("clickcount"));
            long complaintsVal = toLong(r.get("complaintcount"));
            Map<String, Object> item = new HashMap<>();
            item.put("periodLabel", r.get("periodlabel"));
            item.put("periodStart", r.get("periodstart") != null ? r.get("periodstart").toString() : null);
            item.put("periodEnd", r.get("periodend") != null ? r.get("periodend").toString() : null);
            item.put("sentCount", sent);
            item.put("deliveredCount", deliveredVal);
            item.put("bounceCount", toLong(r.get("bouncecount")));
            item.put("complaintCount", complaintsVal);
            item.put("openCount", opensVal);
            item.put("clickCount", clicksVal);
            item.put("deliveryRate", rate(deliveredVal, sent));
            item.put("openRate", rate(opensVal, deliveredVal));
            item.put("clickRate", rate(clicksVal, deliveredVal));
            item.put("complaintRate", rate(complaintsVal, sent));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getTenantReputation() {
        List<Map<String, Object>> rows = monitoringRepository.selectTenantDailySummary();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long total = toLong(r.get("totalsent"));
            long deliveredVal = toLong(r.get("delivered"));
            long bounced = toLong(r.get("bounced"));
            long complained = toLong(r.get("complained"));
            Map<String, Object> item = new HashMap<>();
            item.put("tenantId", r.get("tenantid"));
            item.put("totalSent", total);
            item.put("delivered", deliveredVal);
            item.put("bounced", bounced);
            item.put("complained", complained);
            item.put("deliveryRate", rate(deliveredVal, total));
            item.put("bounceRate", rate(bounced, total));
            item.put("complaintRate", rate(complained, total));
            result.add(item);
        }
        return result;
    }

    /**
     * CloudWatch 테넌트별 SES 메트릭 조회 (API Gateway 경유).
     */
    public Map<String, Object> getTenantMetrics(String tenantId, int period) throws Exception {
        // Get tenant config set name
        var tenant = monitoringRepository.selectTenantConfigSetName(tenantId);
        String configSetName = tenant != null ? (String) tenant.get("configsetname") : "tenant-" + tenantId;

        String jsonBody = gson.toJson(Map.of(
            "action", "GET_TENANT_METRICS",
            "tenantId", tenantId,
            "configSetName", configSetName,
            "period", period
        ));
        var response = apiGatewayClient.post("/tenant-setup", jsonBody);
        return gson.fromJson(response.body(),
            new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
    }

    public Map<String, Object> getSesQuota() throws Exception {
        String jsonBody = gson.toJson(Map.of("action", "GET_ACCOUNT"));
        var response = apiGatewayClient.post("/tenant-setup", jsonBody);
        return gson.fromJson(response.body(),
                new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
    }

    double rate(long numerator, long denominator) {
        if (denominator == 0) return 0.0;
        return Math.round(numerator * 1000.0 / denominator) / 10.0;
    }

    long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }
}
