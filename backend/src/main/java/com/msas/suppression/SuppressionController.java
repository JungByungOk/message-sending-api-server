package com.msas.suppression;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Suppression", description = "수신 거부 목록 관리 API")
@RestController
@RequestMapping("/suppression")
@RequiredArgsConstructor
public class SuppressionController {

    private final SuppressionService suppressionService;

    @Operation(summary = "수신 거부 목록 조회", description = "테넌트의 수신 거부 목록을 페이징 조회합니다.")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Map<String, Object>> getSuppressions(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SuppressionEntity> suppressions = suppressionService.getSuppressions(tenantId, page, size);
        int total = suppressionService.countSuppressions(tenantId);
        return ResponseEntity.ok(Map.of(
                "totalCount", total,
                "suppressions", suppressions
        ));
    }

    @Operation(summary = "수신 거부 제거", description = "테넌트의 수신 거부 목록에서 이메일을 제거합니다.")
    @DeleteMapping("/tenant/{tenantId}/{email}")
    public ResponseEntity<Void> removeSuppression(
            @PathVariable String tenantId,
            @PathVariable String email) {
        suppressionService.removeSuppression(tenantId, email);
        return ResponseEntity.noContent().build();
    }
}
