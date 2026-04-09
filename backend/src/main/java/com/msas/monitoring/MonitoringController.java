package com.msas.monitoring;

import com.google.gson.Gson;
import com.msas.settings.service.ApiGatewayClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "모니터링", description = "발송 통계 및 모니터링 API")
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final MonitoringRepository monitoringRepository;
    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    @Operation(summary = "대시보드 요약 통계")
    @GetMapping("/summary")
    public ResponseEntity<MonitoringSummaryDTO> getSummary() {
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
        dto.setDeliveryRate(sentCount > 0 ? Math.round(deliveredCount * 1000.0 / sentCount) / 10.0 : 0.0);
        dto.setBounceRate(sentCount > 0 ? Math.round(bounceCount * 1000.0 / sentCount) / 10.0 : 0.0);
        dto.setRunningBatchCount(toLong(batchStats.get("runningbatchcount")));
        dto.setPendingBatchCount(toLong(batchStats.get("pendingbatchcount")));

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "시간대별 발송량")
    @GetMapping("/hourly")
    public ResponseEntity<List<Map<String, Object>>> getHourlyStats(
            @RequestParam(required = false) String date) {
        String targetDate = (date != null && !date.isBlank()) ? date : LocalDate.now().toString();
        List<Map<String, Object>> result = monitoringRepository.selectHourlyStats(targetDate);
        Map<Integer, Long> hourMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            int hour = toInt(row.get("hour"));
            hourMap.put(hour, toLong(row.get("sentcount")));
        }
        List<Map<String, Object>> full = new java.util.ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("hour", h);
            slot.put("sentCount", hourMap.getOrDefault(h, 0L));
            full.add(slot);
        }
        return ResponseEntity.ok(full);
    }

    @Operation(summary = "상태별 발송 건수 (최근 7일)")
    @GetMapping("/status-summary")
    public ResponseEntity<List<Map<String, Object>>> getStatusSummary() {
        return ResponseEntity.ok(monitoringRepository.selectStatusSummary());
    }

    @Operation(summary = "반송 / 스팸신고 목록")
    @GetMapping("/bounces")
    public ResponseEntity<Map<String, Object>> getBounceList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = monitoringRepository.selectBounceList(offset, size);
        long total = monitoringRepository.countBounceList();
        List<Map<String, Object>> list = new java.util.ArrayList<>();
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
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "배치 현황 목록")
    @GetMapping("/batches")
    public ResponseEntity<List<Map<String, Object>>> getBatchList() {
        List<Map<String, Object>> rows = monitoringRepository.selectBatchList();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
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
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "최근 캠페인 통계")
    @GetMapping("/recent-campaign")
    public ResponseEntity<RecentCampaignDTO> getRecentCampaign() {
        Map<String, Object> batch = monitoringRepository.selectRecentBatch();
        if (batch == null) return ResponseEntity.ok(new RecentCampaignDTO());

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

        // 이전 캠페인 비교
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
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "주간/월간 트렌드 통계")
    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getTrend(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "12") int count) {
        int safeCount = Math.min(Math.max(count, 1), 24);
        List<Map<String, Object>> rows;
        if ("monthly".equalsIgnoreCase(period)) {
            rows = monitoringRepository.selectMonthlyTrend(safeCount);
        } else {
            rows = monitoringRepository.selectWeeklyTrend(safeCount);
        }
        // PostgreSQL returns lowercase keys → rebuild with camelCase
        List<Map<String, Object>> result = new java.util.ArrayList<>();
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
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "월별 비용 추정 (AWS 서비스별)")
    @GetMapping("/cost")
    public ResponseEntity<Map<String, Object>> getCostEstimate(
            @RequestParam(defaultValue = "6") int months) {
        int safeMonths = Math.min(Math.max(months, 1), 24);
        List<Map<String, Object>> monthlyCounts = monitoringRepository.selectMonthlySendCounts(safeMonths);
        List<Map<String, Object>> monthlyEvents = monitoringRepository.selectMonthlyEventCounts(safeMonths);

        Map<String, Long> eventCountByMonth = new HashMap<>();
        for (Map<String, Object> e : monthlyEvents) {
            eventCountByMonth.put((String) e.get("month"), toLong(e.get("eventcount")));
        }

        double sesPricePerEmail = 0.0001;
        double lambdaPricePerInvoke = 0.0000002;
        double lambdaGbSecPrice = 0.0000166667;
        double lambdaAvgDurationSec = 0.5;
        double lambdaMemoryGb = 0.25;
        double dynamoWritePrice = 0.00000125;
        double dynamoReadPrice = 0.00000025;
        double sqsPricePerRequest = 0.0000004;
        double snsPricePerPublish = 0.0000005;
        double apiGwPricePerRequest = 0.0000035;

        List<Map<String, Object>> monthlyBreakdown = new java.util.ArrayList<>();
        double[] totals = new double[7];
        long totalSent = 0;

        for (Map<String, Object> row : monthlyCounts) {
            long sent = toLong(row.get("totalsent"));
            long events = eventCountByMonth.getOrDefault((String) row.get("month"), 0L);
            totalSent += sent;

            long lambdaInvocations = sent + events;
            double lambdaComputeCost = lambdaInvocations * lambdaAvgDurationSec * lambdaMemoryGb * lambdaGbSecPrice;
            double lambdaInvokeCost = lambdaInvocations * lambdaPricePerInvoke;
            double lambdaCost = round2(lambdaComputeCost + lambdaInvokeCost);
            double dynamoCost = round2(sent * 3 * dynamoWritePrice + sent * 2 * dynamoReadPrice);
            double sqsCost = round2(sent * 3 * sqsPricePerRequest);
            double snsCost = round2(events * snsPricePerPublish);
            double apiGwCost = round2(sent * apiGwPricePerRequest);
            double sesCost = round2(sent * sesPricePerEmail);
            double monthTotal = round2(sesCost + lambdaCost + dynamoCost + sqsCost + snsCost + apiGwCost);

            totals[0] += sesCost;
            totals[1] += lambdaCost;
            totals[2] += dynamoCost;
            totals[3] += sqsCost;
            totals[4] += snsCost;
            totals[5] += apiGwCost;
            totals[6] += monthTotal;

            Map<String, Object> item = new HashMap<>();
            item.put("month", row.get("month"));
            item.put("totalSent", sent);
            item.put("eventCount", events);
            item.put("deliveredCount", toLong(row.get("deliveredcount")));
            item.put("bounceCount", toLong(row.get("bouncecount")));
            item.put("complaintCount", toLong(row.get("complaintcount")));
            item.put("sesCost", sesCost);
            item.put("lambdaCost", lambdaCost);
            item.put("dynamoCost", dynamoCost);
            item.put("sqsCost", sqsCost);
            item.put("snsCost", snsCost);
            item.put("apiGwCost", apiGwCost);
            item.put("totalCost", monthTotal);
            monthlyBreakdown.add(item);
        }

        List<Map<String, Object>> services = List.of(
            svcInfo("Amazon SES", "이메일 발송", "$0.10 / 1,000건", round2(totals[0])),
            svcInfo("AWS Lambda", "email-sender, event-processor 등 5개 함수", "$0.20 / 100만 호출 + 컴퓨팅", round2(totals[1])),
            svcInfo("Amazon DynamoDB", "ems-send-results, ems-tenant-config, ems-idempotency", "$1.25 / 100만 WCU", round2(totals[2])),
            svcInfo("Amazon SQS", "ems-send-queue + DLQ", "$0.40 / 100만 요청", round2(totals[3])),
            svcInfo("Amazon SNS", "ems-ses-events 토픽", "$0.50 / 100만 발행", round2(totals[4])),
            svcInfo("Amazon API Gateway", "ems-api (REST)", "$3.50 / 100만 호출", round2(totals[5]))
        );

        Map<String, Object> result = new HashMap<>();
        result.put("monthlyBreakdown", monthlyBreakdown);
        result.put("services", services);
        result.put("totalSent", totalSent);
        result.put("totalCost", round2(totals[6]));
        result.put("currency", "USD");
        result.put("note", "사용량 기반 추정치입니다. 실제 비용은 AWS Cost Explorer에서 Project=ems 태그로 확인하세요.");
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> svcInfo(String name, String description, String pricing, double cost) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("pricing", pricing);
        m.put("cost", cost);
        return m;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    @Operation(summary = "테넌트별 오늘 발송 요약 (평판 모니터링)")
    @GetMapping("/tenant-reputation")
    public ResponseEntity<List<Map<String, Object>>> getTenantReputation() {
        List<Map<String, Object>> rows = monitoringRepository.selectTenantDailySummary();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
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
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "SES 계정 발송 한도 조회")
    @GetMapping("/ses-quota")
    public ResponseEntity<Map<String, Object>> getSesQuota() {
        try {
            String jsonBody = gson.toJson(Map.of("action", "GET_ACCOUNT"));
            var response = apiGatewayClient.post("/tenant-setup", jsonBody);
            Map<String, Object> result = gson.fromJson(response.body(),
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[MonitoringController] SES 계정 정보 조회 실패.", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) return 0.0;
        return Math.round(numerator * 1000.0 / denominator) / 10.0;
    }

    private long toLong(Object val) {
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
