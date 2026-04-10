package com.msas.monitoring;

import com.google.gson.Gson;
import com.msas.settings.service.ApiGatewayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CostEstimateService {

    private final MonitoringRepository monitoringRepository;
    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    private static final double SES_PRICE_PER_EMAIL = 0.0001;
    private static final double LAMBDA_PRICE_PER_INVOKE = 0.0000002;
    private static final double LAMBDA_GB_SEC_PRICE = 0.0000166667;
    private static final double LAMBDA_AVG_DURATION_SEC = 0.5;
    private static final double LAMBDA_MEMORY_GB = 0.25;
    private static final double DYNAMO_WRITE_PRICE = 0.00000125;
    private static final double DYNAMO_READ_PRICE = 0.00000025;
    private static final double SQS_PRICE_PER_REQUEST = 0.0000004;
    private static final double SNS_PRICE_PER_PUBLISH = 0.0000005;
    private static final double API_GW_PRICE_PER_REQUEST = 0.0000035;

    public Map<String, Object> getCostEstimate(int months) {
        int safeMonths = Math.min(Math.max(months, 1), 24);
        List<Map<String, Object>> monthlyCounts = monitoringRepository.selectMonthlySendCounts(safeMonths);
        List<Map<String, Object>> monthlyEvents = monitoringRepository.selectMonthlyEventCounts(safeMonths);

        Map<String, Long> eventCountByMonth = new HashMap<>();
        for (Map<String, Object> e : monthlyEvents) {
            eventCountByMonth.put((String) e.get("month"), toLong(e.get("eventcount")));
        }

        List<Map<String, Object>> monthlyBreakdown = new ArrayList<>();
        double[] totals = new double[7];
        long totalSent = 0;

        for (Map<String, Object> row : monthlyCounts) {
            long sent = toLong(row.get("totalsent"));
            long events = eventCountByMonth.getOrDefault((String) row.get("month"), 0L);
            totalSent += sent;

            long lambdaInvocations = sent + events;
            double lambdaComputeCost = lambdaInvocations * LAMBDA_AVG_DURATION_SEC * LAMBDA_MEMORY_GB * LAMBDA_GB_SEC_PRICE;
            double lambdaInvokeCost = lambdaInvocations * LAMBDA_PRICE_PER_INVOKE;
            double lambdaCost = round2(lambdaComputeCost + lambdaInvokeCost);
            double dynamoCost = round2(sent * 3 * DYNAMO_WRITE_PRICE + sent * 2 * DYNAMO_READ_PRICE);
            double sqsCost = round2(sent * 3 * SQS_PRICE_PER_REQUEST);
            double snsCost = round2(events * SNS_PRICE_PER_PUBLISH);
            double apiGwCost = round2(sent * API_GW_PRICE_PER_REQUEST);
            double sesCost = round2(sent * SES_PRICE_PER_EMAIL);
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
        return result;
    }

    /**
     * Cost Explorer 실 비용 조회 (API Gateway 경유).
     * 실패 시 예외를 던져 호출자가 폴백 처리.
     */
    public Map<String, Object> getRealCost(String startDate, String endDate) throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("action", "GET_COST");
        if (startDate != null) payload.put("startDate", startDate);
        if (endDate != null) payload.put("endDate", endDate);

        String jsonBody = gson.toJson(payload);
        var response = apiGatewayClient.post("/tenant-setup", jsonBody);
        return gson.fromJson(response.body(),
            new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
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

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
    }
}
