package com.msas.ses.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.msas.common.tenant.TenantContext;
import com.msas.ses.dto.*;
import com.msas.ses.repository.EmailResultRepository;
import com.msas.settings.service.ApiGatewayClient;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import com.msas.tenant.service.SenderValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Tag(name = "AWS SES", description = "이메일 발송 테스트 API (API Gateway 경유)")
@RestController
@RequestMapping("/ses")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final ApiGatewayClient apiGatewayClient;
    private final SenderValidationService senderValidationService;
    private final EmailResultRepository emailResultRepository;
    private final TenantRepository tenantRepository;
    private final com.msas.ses.repository.TemplateTenantRepository templateTenantRepository;
    private final RateLimiter sesRateLimiter;
    private final Gson gson = new Gson();

    /**
     * Authorization 헤더에서 테넌트 ID를 추출합니다.
     * TenantContext가 설정되어 있으면 그것을 사용하고,
     * 없으면 Authorization 헤더의 API Key로 직접 조회합니다.
     */
    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) return tenantId;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String apiKey = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
            TenantEntity tenant = tenantRepository.selectTenantByApiKey(apiKey);
            if (tenant != null) return tenant.getTenantId();
        }
        return null;
    }

    @Operation(summary = "텍스트 이메일 발송 테스트", description = "API Gateway를 통해 HTML 본문 기반 이메일을 발송합니다.")
    @PostMapping("/text-mail")
    public ResponseEntity<ResponseBasicEmailDTO> sendEmail(@Valid @RequestBody RequestBasicEmailDto requestBasicEmailDTO,
                                                            HttpServletRequest request) {
        senderValidationService.validateSender(requestBasicEmailDTO.getFrom());
        String tenantId = resolveTenantId(request);

        // 1. DB에 발송 기록 생성 (MST + DTL per recipient)
        EmailSendMasterInsertDTO master = new EmailSendMasterInsertDTO();
        master.setEmailTypCd("TEXT");
        master.setSendDivCd("H");
        master.setTenantId(tenantId);
        emailResultRepository.insertEmailSendMaster(master);

        String lastCorrelationId = null;
        int successCount = 0;
        int failCount = 0;

        for (String recipient : requestBasicEmailDTO.getTo()) {
            String correlationId = java.util.UUID.randomUUID().toString();

            EmailSendDetailInsertDTO detail = new EmailSendDetailInsertDTO();
            detail.setEmailSendSeq(master.getEmailSendSeq());
            detail.setSendStsCd("Queued");
            detail.setCorrelationId(correlationId);
            detail.setRcvEmailAddr(recipient);
            detail.setSendEmailAddr(requestBasicEmailDTO.getFrom());
            detail.setEmailTitle(requestBasicEmailDTO.getSubject());
            detail.setEmailCts(requestBasicEmailDTO.getBody());
            detail.setTenantId(tenantId);
            emailResultRepository.insertEmailSendDetail(detail);

            try {
                Map<String, Object> sendPayload = new java.util.LinkedHashMap<>();
                sendPayload.put("tenantId", tenantId);
                sendPayload.put("correlationId", correlationId);
                sendPayload.put("from", requestBasicEmailDTO.getFrom());
                sendPayload.put("to", List.of(recipient));
                sendPayload.put("subject", requestBasicEmailDTO.getSubject());
                sendPayload.put("body", requestBasicEmailDTO.getBody());
                List<Map<String, String>> tags = new java.util.ArrayList<>();
                tags.add(Map.of("name", "correlation_id", "value", correlationId));
                if (requestBasicEmailDTO.getTags() != null) {
                    requestBasicEmailDTO.getTags().forEach(t -> tags.add(Map.of("name", t.getName(), "value", t.getValue())));
                }
                sendPayload.put("tags", tags);
                String jsonBody = gson.toJson(sendPayload);
                HttpResponse<String> response = apiGatewayClient.post("/send-email", jsonBody);

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    Map<String, String> result = gson.fromJson(response.body(),
                            new com.google.gson.reflect.TypeToken<Map<String, String>>() {}.getType());
                    String messageId = result.getOrDefault("messageId", "");
                    emailResultRepository.updateEmailSendDetailAfterSend(detail.getEmailSendDtlSeq(), "Sending", detail.getCorrelationId(), messageId);
                    lastCorrelationId = correlationId;
                    successCount++;

                    log.info("[EmailController] 텍스트 이메일 발송 완료. (tenantId: {}, from: {}, to: {}, messageId: {})",
                            tenantId, requestBasicEmailDTO.getFrom(), recipient, messageId);
                } else {
                    emailResultRepository.updateEmailSendDetailAfterSend(detail.getEmailSendDtlSeq(), "Error", detail.getCorrelationId(), null);
                    failCount++;
                    log.warn("[EmailController] 텍스트 이메일 발송 실패. (tenantId: {}, to: {}, HTTP {})",
                            tenantId, recipient, response.statusCode());
                }
            } catch (Exception e) {
                emailResultRepository.updateEmailSendDetailAfterSend(detail.getEmailSendDtlSeq(), "Error", detail.getCorrelationId(), null);
                failCount++;
                log.error("[EmailController] 텍스트 이메일 발송 실패. (tenantId: {}, to: {})",
                        tenantId, recipient, e);
            }
        }

        if (successCount == 0) {
            throw new RuntimeException("모든 수신자에 대한 이메일 발송이 실패했습니다.");
        }

        log.info("[EmailController] 텍스트 이메일 발송 완료. (총: {}, 성공: {}, 실패: {})", requestBasicEmailDTO.getTo().size(), successCount, failCount);
        ResponseBasicEmailDTO dto = new ResponseBasicEmailDTO();
        dto.setMessageId(lastCorrelationId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "템플릿 이메일 발송 테스트", description = "API Gateway를 통해 템플릿 기반 이메일을 발송합니다.")
    @PostMapping("/templated-mail")
    public ResponseEntity<ResponseTemplatedEmailDTO> sendTemplatedEmail(@Valid @RequestBody RequestTemplatedEmailDto requestTemplatedEmailDto,
                                                                         HttpServletRequest request) {
        senderValidationService.validateSender(requestTemplatedEmailDto.getFrom());
        String tenantId = resolveTenantId(request);

        // 1. DB에 발송 기록 생성
        EmailSendMasterInsertDTO master = new EmailSendMasterInsertDTO();
        master.setEmailTypCd("TEMPLATE");
        master.setSendDivCd("P");
        master.setTenantId(tenantId);
        emailResultRepository.insertEmailSendMaster(master);

        String lastCorrelationId = null;
        int successCount = 0;
        int failCount = 0;

        for (String recipient : requestTemplatedEmailDto.getTo()) {
            sesRateLimiter.acquire();
            String correlationId = java.util.UUID.randomUUID().toString();

            EmailSendDetailInsertDTO detail = new EmailSendDetailInsertDTO();
            detail.setEmailSendSeq(master.getEmailSendSeq());
            detail.setSendStsCd("Queued");
            detail.setCorrelationId(correlationId);
            detail.setRcvEmailAddr(recipient);
            detail.setSendEmailAddr(requestTemplatedEmailDto.getFrom());
            detail.setEmailTitle(requestTemplatedEmailDto.getSubject() != null && !requestTemplatedEmailDto.getSubject().isEmpty()
                    ? requestTemplatedEmailDto.getSubject()
                    : requestTemplatedEmailDto.getTemplateName());
            detail.setEmailTmpletId(requestTemplatedEmailDto.getTemplateName());
            detail.setTenantId(tenantId);
            emailResultRepository.insertEmailSendDetail(detail);

            try {
                // tenantId를 포함하여 발송
                Map<String, Object> sendPayload = new java.util.LinkedHashMap<>();
                sendPayload.put("tenantId", tenantId);
                sendPayload.put("correlationId", correlationId);
                sendPayload.put("from", requestTemplatedEmailDto.getFrom());
                sendPayload.put("to", List.of(recipient));
                sendPayload.put("templateName", requestTemplatedEmailDto.getTemplateName());
                sendPayload.put("templateData", requestTemplatedEmailDto.getTemplateData());
                if (requestTemplatedEmailDto.getCc() != null) {
                    sendPayload.put("cc", requestTemplatedEmailDto.getCc());
                }
                if (requestTemplatedEmailDto.getBcc() != null) {
                    sendPayload.put("bcc", requestTemplatedEmailDto.getBcc());
                }
                // correlation_id tag 추가 (SES EmailTag → event-processor에서 추출)
                List<Map<String, String>> tags = new java.util.ArrayList<>();
                tags.add(Map.of("name", "correlation_id", "value", correlationId));
                if (requestTemplatedEmailDto.getTags() != null) {
                    requestTemplatedEmailDto.getTags().forEach(t -> tags.add(Map.of("name", t.getName(), "value", t.getValue())));
                }
                sendPayload.put("tags", tags);
                String jsonBody = gson.toJson(sendPayload);
                HttpResponse<String> response = apiGatewayClient.post("/send-email", jsonBody);

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    Map<String, String> result = gson.fromJson(response.body(),
                            new com.google.gson.reflect.TypeToken<Map<String, String>>() {}.getType());
                    String messageId = result.getOrDefault("messageId", "");
                    emailResultRepository.updateEmailSendDetailAfterSend(detail.getEmailSendDtlSeq(), "Sending", detail.getCorrelationId(), messageId);
                    lastCorrelationId = correlationId;
                    successCount++;

                    log.info("[EmailController] 템플릿 이메일 발송 완료. (tenantId: {}, from: {}, to: {}, template: {}, messageId: {})",
                            tenantId, requestTemplatedEmailDto.getFrom(), recipient,
                            requestTemplatedEmailDto.getTemplateName(), messageId);
                } else {
                    emailResultRepository.updateEmailSendDetailAfterSend(detail.getEmailSendDtlSeq(), "Error", detail.getCorrelationId(), null);
                    failCount++;
                    log.warn("[EmailController] 템플릿 이메일 발송 실패. (tenantId: {}, to: {}, template: {}, HTTP {})",
                            tenantId, recipient, requestTemplatedEmailDto.getTemplateName(), response.statusCode());
                }
            } catch (Exception e) {
                emailResultRepository.updateEmailSendDetailAfterSend(detail.getEmailSendDtlSeq(), "Error", detail.getCorrelationId(), null);
                failCount++;
                log.error("[EmailController] 템플릿 이메일 발송 실패. (tenantId: {}, to: {}, template: {})",
                        tenantId, recipient, requestTemplatedEmailDto.getTemplateName(), e);
            }
        }

        if (successCount == 0) {
            throw new RuntimeException("모든 수신자에 대한 이메일 발송이 실패했습니다.");
        }

        log.info("[EmailController] 템플릿 이메일 발송 완료. (총: {}, 성공: {}, 실패: {})", requestTemplatedEmailDto.getTo().size(), successCount, failCount);
        ResponseTemplatedEmailDTO dto = new ResponseTemplatedEmailDTO();
        dto.setMessageId(lastCorrelationId);
        return ResponseEntity.ok(dto);
    }

    // === 템플릿 관리 (API Gateway 경유) ===

    @Operation(summary = "템플릿 생성", description = "API Gateway를 통해 SES 이메일 템플릿을 생성합니다.")
    @PostMapping("/template")
    public ResponseEntity<ResponseTemplatedDTO> createTemplate(@Valid @RequestBody RequestTemplateDto requestTemplateDto) {
        try {
            String jsonBody = gson.toJson(Map.of(
                    "action", "CREATE_TEMPLATE",
                    "templateName", requestTemplateDto.getTemplateName(),
                    "subjectPart", requestTemplateDto.getSubjectPart(),
                    "htmlPart", requestTemplateDto.getHtmlPart(),
                    "textPart", requestTemplateDto.getTextPart()
            ));
            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMsg = extractField(response.body(), "message");
                log.error("[EmailController] 템플릿 생성 실패. Lambda 오류: {} (templateName: {})", errorMsg, requestTemplateDto.getTemplateName());
                throw new RuntimeException("템플릿 생성 실패: " + errorMsg);
            }
            ResponseTemplatedDTO dto = new ResponseTemplatedDTO();
            dto.setAwsRequestId(extractField(response.body(), "awsRequestId"));
            log.info("[EmailController] 템플릿 생성 완료. (templateName: {})", requestTemplateDto.getTemplateName());
            templateTenantRepository.updateSubjectByTemplate(requestTemplateDto.getTemplateName(), requestTemplateDto.getSubjectPart());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EmailController] 템플릿 생성 실패. (templateName: {})",
                    requestTemplateDto.getTemplateName(), e);
            throw new RuntimeException("템플릿 생성 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "템플릿 수정", description = "API Gateway를 통해 SES 이메일 템플릿을 수정합니다.")
    @PatchMapping("/template")
    public ResponseEntity<ResponseTemplatedDTO> updateTemplate(@Valid @RequestBody RequestTemplateDto requestTemplateDto) {
        try {
            String jsonBody = gson.toJson(Map.of(
                    "action", "UPDATE_TEMPLATE",
                    "templateName", requestTemplateDto.getTemplateName(),
                    "subjectPart", requestTemplateDto.getSubjectPart(),
                    "htmlPart", requestTemplateDto.getHtmlPart(),
                    "textPart", requestTemplateDto.getTextPart()
            ));
            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMsg = extractField(response.body(), "message");
                log.error("[EmailController] 템플릿 수정 실패. Lambda 오류: {} (templateName: {})", errorMsg, requestTemplateDto.getTemplateName());
                throw new RuntimeException("템플릿 수정 실패: " + errorMsg);
            }
            ResponseTemplatedDTO dto = new ResponseTemplatedDTO();
            dto.setAwsRequestId(extractField(response.body(), "awsRequestId"));
            log.info("[EmailController] 템플릿 수정 완료. (templateName: {})", requestTemplateDto.getTemplateName());
            templateTenantRepository.updateSubjectByTemplate(requestTemplateDto.getTemplateName(), requestTemplateDto.getSubjectPart());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EmailController] 템플릿 수정 실패. (templateName: {})",
                    requestTemplateDto.getTemplateName(), e);
            throw new RuntimeException("템플릿 수정 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "템플릿 삭제", description = "API Gateway를 통해 SES 이메일 템플릿을 삭제합니다.")
    @DeleteMapping("/template")
    public ResponseEntity<ResponseDeleteTemplatedDTO> deleteTemplate(@Valid @RequestBody RequestDeleteTemplateDto requestDeleteTemplateDto) {
        try {
            String jsonBody = gson.toJson(Map.of(
                    "action", "DELETE_TEMPLATE",
                    "templateName", requestDeleteTemplateDto.getTemplateName()
            ));
            HttpResponse<String> response = apiGatewayClient.post("/tenant-setup", jsonBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMsg = extractField(response.body(), "message");
                log.error("[EmailController] 템플릿 삭제 실패. Lambda 오류: {} (templateName: {})", errorMsg, requestDeleteTemplateDto.getTemplateName());
                throw new RuntimeException("템플릿 삭제 실패: " + errorMsg);
            }
            ResponseDeleteTemplatedDTO dto = new ResponseDeleteTemplatedDTO();
            dto.setAwsRequestId(extractField(response.body(), "awsRequestId"));
            log.info("[EmailController] 템플릿 삭제 완료. (templateName: {})", requestDeleteTemplateDto.getTemplateName());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EmailController] 템플릿 삭제 실패. (templateName: {})",
                    requestDeleteTemplateDto.getTemplateName(), e);
            throw new RuntimeException("템플릿 삭제 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "템플릿 상세 조회", description = "API Gateway를 통해 SES 이메일 템플릿 내용을 조회합니다.")
    @GetMapping("/template")
    public ResponseEntity<Map<String, Object>> getTemplate(@RequestParam String templateName) {
        try {
            HttpResponse<String> response = apiGatewayClient.get(
                    "/tenant-setup?action=GET_TEMPLATE&templateName=" + java.net.URLEncoder.encode(templateName, "UTF-8"));
            Map<String, Object> template = gson.fromJson(response.body(),
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
            // 조회 시 매핑 테이블 subject 동기화
            Object subjectPart = template.get("subjectPart");
            if (subjectPart instanceof String s && !s.isEmpty()) {
                templateTenantRepository.updateSubjectByTemplate(templateName, s);
            }
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("[EmailController] 템플릿 조회 실패. (templateName: {})", templateName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "템플릿 목록 조회", description = "API Gateway를 통해 SES 이메일 템플릿 목록을 조회합니다.")
    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, Object>>> listTemplate() {
        try {
            HttpResponse<String> response = apiGatewayClient.get("/tenant-setup?action=LIST_TEMPLATES");
            List<Map<String, Object>> templates = gson.fromJson(response.body(),
                    new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {}.getType());
            return ResponseEntity.ok(templates != null ? templates : List.of());
        } catch (Exception e) {
            log.error("[EmailController] 템플릿 목록 조회 실패.", e);
            return ResponseEntity.ok(List.of());
        }
    }

    private String extractField(String json, String field) {
        try {
            Map<String, String> map = gson.fromJson(json,
                    new com.google.gson.reflect.TypeToken<Map<String, String>>() {}.getType());
            return map != null ? map.getOrDefault(field, "") : "";
        } catch (Exception e) {
            return json;
        }
    }
}
