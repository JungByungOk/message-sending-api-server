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
}
