package com.msas.ses.configset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sesv2.model.GetConfigurationSetResponse;

import java.util.Map;

@Tag(name = "SES ConfigSet", description = "SES 구성 세트 관리 API")
@RestController
@RequestMapping("/ses/config-set")
@RequiredArgsConstructor
@ConditionalOnBean(SesV2Client.class)
public class SESConfigSetController {

    private final SESConfigSetService sesConfigSetService;

    @Operation(summary = "ConfigSet 생성", description = "테넌트 ID 기반 SES 구성 세트를 생성합니다.")
    @PostMapping
    public ResponseEntity<Map<String, String>> createConfigSet(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String configSetName = sesConfigSetService.createConfigSet(tenantId);
        return new ResponseEntity<>(Map.of("configSetName", configSetName), HttpStatus.CREATED);
    }

    @Operation(summary = "ConfigSet 조회", description = "테넌트 ID로 SES 구성 세트 정보를 조회합니다.")
    @GetMapping("/{tenantId}")
    public ResponseEntity<Map<String, Object>> getConfigSet(@PathVariable String tenantId) {
        GetConfigurationSetResponse response = sesConfigSetService.getConfigSet(tenantId);
        return ResponseEntity.ok(Map.of(
                "configSetName", response.configurationSetName() != null ? response.configurationSetName() : "tenant-" + tenantId,
                "tenantId", tenantId
        ));
    }

    @Operation(summary = "ConfigSet 삭제", description = "테넌트 ID에 해당하는 SES 구성 세트를 삭제합니다.")
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteConfigSet(@PathVariable String tenantId) {
        sesConfigSetService.deleteConfigSet("tenant-" + tenantId);
        return ResponseEntity.noContent().build();
    }
}
