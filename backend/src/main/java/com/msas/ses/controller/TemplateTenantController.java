package com.msas.ses.controller;

import com.msas.ses.repository.TemplateTenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "템플릿-테넌트 매핑", description = "템플릿과 테넌트 간 연결 관리")
@RestController
@RequestMapping("/ses/template-tenant")
@RequiredArgsConstructor
public class TemplateTenantController {

    private final TemplateTenantRepository repository;

    @Operation(summary = "템플릿에 테넌트 매핑 추가")
    @PostMapping
    public ResponseEntity<Map<String, Object>> addMapping(
            @RequestBody Map<String, String> body) {
        String templateName = body.get("templateName");
        String tenantId = body.get("tenantId");
        if (templateName == null || templateName.isBlank() || tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "templateName과 tenantId는 필수입니다."));
        }
        String subject = body.get("subject");
        repository.insertMapping(templateName, tenantId, subject);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @Operation(summary = "템플릿에서 테넌트 매핑 제거")
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> removeMapping(
            @RequestParam String templateName,
            @RequestParam String tenantId) {
        repository.deleteMapping(templateName, tenantId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @Operation(summary = "템플릿의 매핑된 테넌트 목록 조회")
    @GetMapping("/by-template")
    public ResponseEntity<List<String>> getTenantsByTemplate(
            @RequestParam String templateName) {
        return ResponseEntity.ok(repository.selectTenantsByTemplate(templateName));
    }

    @Operation(summary = "테넌트의 매핑된 템플릿 목록 조회")
    @GetMapping("/by-tenant")
    public ResponseEntity<List<String>> getTemplatesByTenant(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(repository.selectTemplatesByTenant(tenantId));
    }

    @Operation(summary = "전체 매핑 조회 (템플릿별 테넌트 그룹)")
    @GetMapping("/all")
    public ResponseEntity<Map<String, List<String>>> getAllMappings() {
        List<Map<String, Object>> rows = repository.selectAllMappings();
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String templateName = (String) row.get("templatename");
            String tenantId = (String) row.get("tenantid");
            result.computeIfAbsent(templateName, k -> new ArrayList<>()).add(tenantId);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "템플릿의 테넌트 매핑 일괄 업데이트")
    @PutMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> updateMappings(
            @RequestBody Map<String, Object> body) {
        String templateName = (String) body.get("templateName");
        if (templateName == null || templateName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "templateName은 필수입니다."));
        }
        @SuppressWarnings("unchecked")
        List<String> tenantIds = (List<String>) body.get("tenantIds");

        repository.deleteMappingsByTemplate(templateName);
        if (tenantIds != null) {
            for (String tenantId : tenantIds) {
                if (tenantId != null && !tenantId.isBlank()) {
                    repository.insertMapping(templateName, tenantId, null);
                }
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "count", tenantIds != null ? tenantIds.size() : 0));
    }
}
