package com.msas.ses.configset;

import com.msas.settings.service.ApiGatewayClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
import java.util.Map;

@Tag(name = "SES ConfigSet", description = "SES 구성 세트 관리 API (API Gateway 경유)")
@RestController
@RequestMapping("/ses/config-set")
@RequiredArgsConstructor
@Slf4j
public class SESConfigSetController {

    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    @Operation(summary = "ConfigSet 생성", description = "API Gateway를 통해 테넌트용 SES ConfigSet을 생성합니다.")
    @PostMapping
    public ResponseEntity<Map<String, String>> createConfigSet(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        try {
            String jsonBody = gson.toJson(Map.of("tenantId", tenantId, "action", "CREATE_CONFIGSET"));
            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);
            Map<String, Object> result = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>() {}.getType());
            String configSetName = result.getOrDefault("configSetName", "tenant-" + tenantId).toString();
            return new ResponseEntity<>(Map.of("configSetName", configSetName), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("ConfigSet 생성 실패. (tenantId: {})", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "ConfigSet 조회", description = "API Gateway를 통해 테넌트용 ConfigSet 정보를 조회합니다.")
    @GetMapping("/{tenantId}")
    public ResponseEntity<Map<String, Object>> getConfigSet(@PathVariable String tenantId) {
        try {
            HttpResponse<String> response = apiGatewayClient.get(
                    "/tenant-setup?tenantId=" + tenantId + "&action=GET_CONFIGSET");
            Map<String, Object> result = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>() {}.getType());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("ConfigSet 조회 실패. (tenantId: {})", tenantId, e);
            return ResponseEntity.ok(Map.of("configSetName", "tenant-" + tenantId, "tenantId", tenantId));
        }
    }

    @Operation(summary = "ConfigSet 삭제", description = "API Gateway를 통해 테넌트용 ConfigSet을 삭제합니다.")
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteConfigSet(@PathVariable String tenantId) {
        try {
            apiGatewayClient.delete("/tenant-setup?tenantId=" + tenantId + "&action=DELETE_CONFIGSET");
        } catch (Exception e) {
            log.error("ConfigSet 삭제 실패. (tenantId: {})", tenantId, e);
        }
        return ResponseEntity.noContent().build();
    }
}
