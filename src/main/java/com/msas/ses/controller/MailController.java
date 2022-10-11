package com.msas.ses.controller;

import com.google.gson.Gson;
import com.msas.ses.dto.EmailDto;
import com.msas.ses.service.SESMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AWS SES 를 이용하여 이메일 전송 요청을 처리
 */
@RestController
@RequestMapping("/ses")
@RequiredArgsConstructor
public class MailController {

    private final SESMailService SESMailService;

    @PostMapping("/send-email")
    public String sendEmail(@RequestBody EmailDto emailDTO) {
        return new Gson().toJson(SESMailService.sendEmail(emailDTO));
    }

    /**
     * SendTemplatedEmailResult sendTemplatedEmail(SendTemplatedEmailRequest sendTemplatedEmailRequest);
     */
    @PostMapping("/send-templated-email")
    public String sendTemplatedEmail() {
        return "";
    }

    /**
     * CreateTemplateResult createTemplate(CreateTemplateRequest createTemplateRequest);
     */
    @PostMapping("/template")
    public String createTemplate() {
        return "";
    }

    /**
     * UpdateTemplateResult updateTemplate(UpdateTemplateRequest updateTemplateRequest);
     */
    @PatchMapping("/template")
    public String updateTemplate() {
        return "";
    }

    /**
     * DeleteTemplateResult deleteTemplate(DeleteTemplateRequest deleteTemplateRequest);
     */
    @DeleteMapping("/template")
    public String deleteTemplate() {
        return "";
    }

}
