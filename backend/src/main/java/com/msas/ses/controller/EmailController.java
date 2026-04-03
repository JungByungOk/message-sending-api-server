package com.msas.ses.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import com.msas.ses.service.SESMailService;
import software.amazon.awssdk.services.ses.model.TemplateMetadata;
import com.msas.ses.dto.*;
import com.msas.ses.service.SESMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "AWS SES", description = "AWS SES 이메일 발송 및 템플릿 관리 API")
@RestController
@RequestMapping("/ses")
@RequiredArgsConstructor
@ConditionalOnBean(SESMailService.class)
public class EmailController {

    private final SESMailService sesMailService;

    @Operation(summary = "텍스트 이메일 발송", description = "HTML 본문 기반 이메일을 발송합니다.")
    @PostMapping("/text-mail")
    public ResponseEntity<ResponseBasicEmailDTO> sendEmail(@Valid @RequestBody RequestBasicEmailDto requestBasicEmailDTO) {

        String messageId = sesMailService.sendEmail(requestBasicEmailDTO);

        ResponseBasicEmailDTO responseBasicEmailDTO = new ResponseBasicEmailDTO();
        responseBasicEmailDTO.setMessageId(messageId);

        return new ResponseEntity<>(responseBasicEmailDTO, HttpStatus.OK);
    }

    @Operation(summary = "템플릿 이메일 발송", description = "AWS SES 템플릿 기반으로 이메일을 발송합니다. CC/BCC 지원.")
    @PostMapping("/templated-mail")
    public ResponseEntity<ResponseTemplatedEmailDTO> sendTemplatedEmail(@Valid @RequestBody RequestTemplatedEmailDto requestTemplatedEmailDto) {
        String messageId = sesMailService.sendTemplatedEmail(requestTemplatedEmailDto);

        ResponseTemplatedEmailDTO responseTemplatedEmailDTO = new ResponseTemplatedEmailDTO();
        responseTemplatedEmailDTO.setMessageId(messageId);

        return new ResponseEntity<>(responseTemplatedEmailDTO, HttpStatus.OK);
    }

    @Operation(summary = "템플릿 생성", description = "AWS SES 이메일 템플릿을 새로 등록합니다.")
    @PostMapping("/template")
    public ResponseEntity<ResponseTemplatedDTO> createTemplate(@Valid @RequestBody RequestTemplateDto requestTemplateDto) {
        String awsRequestId = sesMailService.createTemplateEmail(requestTemplateDto);

        ResponseTemplatedDTO responseTemplatedDTO = new ResponseTemplatedDTO();
        responseTemplatedDTO.setAwsRequestId(awsRequestId);

        return new ResponseEntity<>(responseTemplatedDTO, HttpStatus.OK);
    }

    @Operation(summary = "템플릿 수정", description = "기존 AWS SES 이메일 템플릿을 수정합니다.")
    @PatchMapping("/template")
    public ResponseEntity<ResponseTemplatedDTO> updateTemplate(@Valid @RequestBody RequestTemplateDto requestTemplateDto) {
        String awsRequestId = sesMailService.updateTemplateEmail(requestTemplateDto);

        ResponseTemplatedDTO responseTemplatedDTO = new ResponseTemplatedDTO();
        responseTemplatedDTO.setAwsRequestId(awsRequestId);

        return new ResponseEntity<>(responseTemplatedDTO, HttpStatus.OK);
    }

    @Operation(summary = "템플릿 삭제", description = "AWS SES 이메일 템플릿을 삭제합니다.")
    @DeleteMapping("/template")
    public ResponseEntity<ResponseDeleteTemplatedDTO> deleteTemplate(@Valid @RequestBody RequestDeleteTemplateDto requestDeleteTemplateDto) {
        String awsRequestId = sesMailService.deleteTemplate(requestDeleteTemplateDto);

        ResponseDeleteTemplatedDTO responseDeleteTemplatedDTO = new ResponseDeleteTemplatedDTO();
        responseDeleteTemplatedDTO.setAwsRequestId(awsRequestId);

        return new ResponseEntity<>(responseDeleteTemplatedDTO, HttpStatus.OK);
    }

    @Operation(summary = "템플릿 목록 조회", description = "등록된 모든 AWS SES 이메일 템플릿 목록을 조회합니다.")
    @GetMapping("/templates")
    public ResponseEntity<List<TemplateMetadata>> listTemplate() {
        return new ResponseEntity<List<TemplateMetadata>>(sesMailService.listTemplates(), HttpStatus.OK);
    }

}
