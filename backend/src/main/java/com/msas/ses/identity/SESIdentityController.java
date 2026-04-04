package com.msas.ses.identity;

import com.msas.ses.identity.DkimRecordsDTO;
import com.msas.settings.service.ApiGatewayClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "SES Identity", description = "SES 도메인 아이덴티티 관리 API (API Gateway 경유)")
@RestController
@RequestMapping("/ses/identity")
@RequiredArgsConstructor
@Slf4j
public class SESIdentityController {

    private final ApiGatewayClient apiGatewayClient;
    private final Gson gson = new Gson();

    @Operation(summary = "도메인 아이덴티티 생성", description = "API Gateway를 통해 SES에 도메인 아이덴티티를 등록합니다.")
    @PostMapping
    public ResponseEntity<DkimRecordsDTO> createDomainIdentity(@RequestBody Map<String, String> request) {
        String domain = request.get("domain");
        try {
            String jsonBody = gson.toJson(Map.of("domain", domain, "action", "CREATE_IDENTITY"));
            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);
            Map<String, Object> result = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>() {}.getType());
            return ResponseEntity.ok(parseDkimRecords(result, domain));
        } catch (Exception e) {
            log.error("SES Identity 생성 실패. (domain: {})", domain, e);
            return ResponseEntity.ok(new DkimRecordsDTO(domain, "FAILED", List.of()));
        }
    }

    @Operation(summary = "도메인 인증 상태 조회", description = "API Gateway를 통해 도메인 인증 상태를 조회합니다.")
    @GetMapping("/{domain}")
    public ResponseEntity<DkimRecordsDTO> getDomainVerificationStatus(@PathVariable String domain) {
        try {
            HttpResponse<String> response = apiGatewayClient.get(
                    "/tenant-setup?domain=" + domain + "&action=DKIM_STATUS");
            Map<String, Object> result = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>() {}.getType());
            return ResponseEntity.ok(parseDkimRecords(result, domain));
        } catch (Exception e) {
            log.error("SES Identity 조회 실패. (domain: {})", domain, e);
            return ResponseEntity.ok(new DkimRecordsDTO(domain, "UNKNOWN", List.of()));
        }
    }

    @Operation(summary = "도메인 아이덴티티 삭제", description = "API Gateway를 통해 도메인 아이덴티티를 삭제합니다.")
    @DeleteMapping("/{domain}")
    public ResponseEntity<Void> deleteDomainIdentity(@PathVariable String domain) {
        try {
            apiGatewayClient.delete("/tenant-setup?domain=" + domain + "&action=DELETE_IDENTITY");
        } catch (Exception e) {
            log.error("SES Identity 삭제 실패. (domain: {})", domain, e);
        }
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private DkimRecordsDTO parseDkimRecords(Map<String, Object> result, String domain) {
        String status = result.getOrDefault("verificationStatus", "PENDING").toString();
        List<DkimRecordDTO> records = new ArrayList<>();
        Object dkimList = result.get("dkimRecords");
        if (dkimList instanceof List) {
            for (Object item : (List<?>) dkimList) {
                if (item instanceof Map) {
                    Map<String, String> rec = (Map<String, String>) item;
                    records.add(new DkimRecordDTO(
                            rec.getOrDefault("name", ""),
                            rec.getOrDefault("type", "CNAME"),
                            rec.getOrDefault("value", "")));
                }
            }
        }
        return new DkimRecordsDTO(domain, status, records);
    }
}
