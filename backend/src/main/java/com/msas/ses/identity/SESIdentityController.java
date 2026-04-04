package com.msas.ses.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.util.Map;

@Tag(name = "SES Identity", description = "SES 도메인 아이덴티티 관리 API")
@RestController
@RequestMapping("/ses/identity")
@RequiredArgsConstructor
@ConditionalOnBean(SesV2Client.class)
public class SESIdentityController {

    private final SESIdentityService sesIdentityService;

    @Operation(summary = "도메인 아이덴티티 생성", description = "SES에 도메인 이메일 아이덴티티를 등록하고 DKIM 레코드를 반환합니다.")
    @PostMapping
    public ResponseEntity<DkimRecordsDTO> createDomainIdentity(@RequestBody Map<String, String> request) {
        String domain = request.get("domain");
        DkimRecordsDTO result = sesIdentityService.createDomainIdentity(domain);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "도메인 인증 상태 조회", description = "도메인의 SES 인증 상태 및 DKIM 레코드를 조회합니다.")
    @GetMapping("/{domain}")
    public ResponseEntity<DkimRecordsDTO> getDomainVerificationStatus(@PathVariable String domain) {
        DkimRecordsDTO result = sesIdentityService.getDomainVerificationStatus(domain);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "도메인 아이덴티티 삭제", description = "SES에서 도메인 이메일 아이덴티티를 삭제합니다.")
    @DeleteMapping("/{domain}")
    public ResponseEntity<Void> deleteDomainIdentity(@PathVariable String domain) {
        sesIdentityService.deleteDomainIdentity(domain);
        return ResponseEntity.noContent().build();
    }
}
