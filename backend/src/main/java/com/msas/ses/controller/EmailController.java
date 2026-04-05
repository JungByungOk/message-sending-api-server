package com.msas.ses.controller;

import com.google.gson.Gson;
import com.msas.ses.dto.*;
import com.msas.settings.service.ApiGatewayClient;
import com.msas.tenant.service.SenderValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final Gson gson = new Gson();

    @Operation(summary = "텍스트 이메일 발송 테스트", description = "API Gateway를 통해 HTML 본문 기반 이메일을 발송합니다.")
    @PostMapping("/text-mail")
    public ResponseEntity<ResponseBasicEmailDTO> sendEmail(@Valid @RequestBody RequestBasicEmailDto requestBasicEmailDTO) {
        senderValidationService.validateSender(requestBasicEmailDTO.getFrom());

        try {
            String jsonBody = gson.toJson(requestBasicEmailDTO);
            HttpResponse<String> response = apiGatewayClient.post("/send-email", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, String> result = gson.fromJson(response.body(),
                        new com.google.gson.reflect.TypeToken<Map<String, String>>() {}.getType());
                ResponseBasicEmailDTO dto = new ResponseBasicEmailDTO();
                dto.setMessageId(result.getOrDefault("messageId", response.body()));
                return ResponseEntity.ok(dto);
            } else {
                log.warn("EmailController - 이메일 발송 실패. (HTTP {}, body: {})", response.statusCode(), response.body());
                throw new RuntimeException("이메일 발송 실패 (HTTP " + response.statusCode() + ")");
            }
        } catch (IllegalStateException e) {
            throw e; // API Gateway 미설정
        } catch (Exception e) {
            log.error("EmailController - 이메일 발송 실패.", e);
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "템플릿 이메일 발송 테스트", description = "API Gateway를 통해 템플릿 기반 이메일을 발송합니다.")
    @PostMapping("/templated-mail")
    public ResponseEntity<ResponseTemplatedEmailDTO> sendTemplatedEmail(@Valid @RequestBody RequestTemplatedEmailDto requestTemplatedEmailDto) {
        senderValidationService.validateSender(requestTemplatedEmailDto.getFrom());

        try {
            String jsonBody = gson.toJson(requestTemplatedEmailDto);
            HttpResponse<String> response = apiGatewayClient.post("/send-email", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, String> result = gson.fromJson(response.body(),
                        new com.google.gson.reflect.TypeToken<Map<String, String>>() {}.getType());
                ResponseTemplatedEmailDTO dto = new ResponseTemplatedEmailDTO();
                dto.setMessageId(result.getOrDefault("messageId", response.body()));
                return ResponseEntity.ok(dto);
            } else {
                throw new RuntimeException("템플릿 이메일 발송 실패 (HTTP " + response.statusCode() + ")");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("EmailController - 템플릿 이메일 발송 실패.", e);
            throw new RuntimeException("템플릿 이메일 발송 실패: " + e.getMessage());
        }
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
            ResponseTemplatedDTO dto = new ResponseTemplatedDTO();
            dto.setAwsRequestId(extractField(response.body(), "awsRequestId"));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("EmailController - 템플릿 생성 실패.", e);
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
            ResponseTemplatedDTO dto = new ResponseTemplatedDTO();
            dto.setAwsRequestId(extractField(response.body(), "awsRequestId"));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("EmailController - 템플릿 수정 실패.", e);
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
            ResponseDeleteTemplatedDTO dto = new ResponseDeleteTemplatedDTO();
            dto.setAwsRequestId(extractField(response.body(), "awsRequestId"));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("EmailController - 템플릿 삭제 실패.", e);
            throw new RuntimeException("템플릿 삭제 실패: " + e.getMessage());
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
            log.error("EmailController - 템플릿 목록 조회 실패.", e);
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
