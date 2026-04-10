package com.msas.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "모니터링", description = "발송 통계 및 모니터링 API")
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final CostEstimateService costEstimateService;

    @Operation(summary = "대시보드 요약 통계")
    @GetMapping("/summary")
    public ResponseEntity<MonitoringSummaryDTO> getSummary() {
        return ResponseEntity.ok(monitoringService.getSummary());
    }

    @Operation(summary = "시간대별 발송량")
    @GetMapping("/hourly")
    public ResponseEntity<List<Map<String, Object>>> getHourlyStats(
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(monitoringService.getHourlyStats(date));
    }

    @Operation(summary = "상태별 발송 건수 (최근 7일)")
    @GetMapping("/status-summary")
    public ResponseEntity<List<Map<String, Object>>> getStatusSummary() {
        return ResponseEntity.ok(monitoringService.getStatusSummary());
    }

    @Operation(summary = "반송 / 스팸신고 목록")
    @GetMapping("/bounces")
    public ResponseEntity<Map<String, Object>> getBounceList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(monitoringService.getBounceList(page, size));
    }

    @Operation(summary = "배치 현황 목록")
    @GetMapping("/batches")
    public ResponseEntity<List<Map<String, Object>>> getBatchList() {
        return ResponseEntity.ok(monitoringService.getBatchList());
    }

    @Operation(summary = "최근 캠페인 통계")
    @GetMapping("/recent-campaign")
    public ResponseEntity<RecentCampaignDTO> getRecentCampaign() {
        return ResponseEntity.ok(monitoringService.getRecentCampaign());
    }

    @Operation(summary = "주간/월간 트렌드 통계")
    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getTrend(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "12") int count) {
        return ResponseEntity.ok(monitoringService.getTrend(period, count));
    }

    @Operation(summary = "월별 비용 추정 (AWS 서비스별)")
    @GetMapping("/cost")
    public ResponseEntity<Map<String, Object>> getCostEstimate(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(costEstimateService.getCostEstimate(months));
    }

    @Operation(summary = "테넌트별 오늘 발송 요약 (평판 모니터링)")
    @GetMapping("/tenant-reputation")
    public ResponseEntity<List<Map<String, Object>>> getTenantReputation() {
        return ResponseEntity.ok(monitoringService.getTenantReputation());
    }

    @Operation(summary = "SES 계정 발송 한도 조회")
    @GetMapping("/ses-quota")
    public ResponseEntity<Map<String, Object>> getSesQuota() {
        try {
            return ResponseEntity.ok(monitoringService.getSesQuota());
        } catch (Exception e) {
            log.error("[MonitoringController] SES 계정 정보 조회 실패.", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "테넌트별 CloudWatch SES 메트릭 조회")
    @GetMapping("/tenant-metrics/{tenantId}")
    public ResponseEntity<Map<String, Object>> getTenantMetrics(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "3600") int period) {
        try {
            return ResponseEntity.ok(monitoringService.getTenantMetrics(tenantId, period));
        } catch (Exception e) {
            log.error("[MonitoringController] 테넌트 메트릭 조회 실패. (tenantId: {})", tenantId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "AWS 실 비용 조회 (Cost Explorer)")
    @GetMapping("/cost/real")
    public ResponseEntity<Map<String, Object>> getRealCost(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            return ResponseEntity.ok(costEstimateService.getRealCost(startDate, endDate));
        } catch (Exception e) {
            log.error("[MonitoringController] Cost Explorer 조회 실패.", e);
            return ResponseEntity.ok(costEstimateService.getCostEstimate(6)); // fallback to estimate
        }
    }
}
