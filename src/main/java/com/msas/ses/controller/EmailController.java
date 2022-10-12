package com.msas.ses.controller;

import com.msas.ses.dto.EmailDto;
import com.msas.ses.dto.EmailResultDTO;
import com.msas.ses.service.SESMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * AWS SES 를 이용하여 이메일 전송 요청을 처리
 */
@RestController
@RequestMapping("/ses")
@RequiredArgsConstructor
public class EmailController {

    private final SESMailService SESMailService;

    @PostMapping("/send-email")
    public ResponseEntity<EmailResultDTO> sendEmail(@Valid @RequestBody EmailDto emailDTO) {

        String messageId = SESMailService.sendEmail(emailDTO);

        EmailResultDTO emailResultDTO = new EmailResultDTO();
        {
            emailResultDTO.setMessageId(messageId);
        }

        return new ResponseEntity<EmailResultDTO>(emailResultDTO, HttpStatus.OK);
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
