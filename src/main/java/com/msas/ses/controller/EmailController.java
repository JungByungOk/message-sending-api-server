package com.msas.ses.controller;

import com.amazonaws.services.simpleemail.model.TemplateMetadata;
import com.msas.ses.dto.*;
import com.msas.ses.service.SESMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * AWS SES 를 이용하여 이메일 전송 요청을 처리
 */
@RestController
@RequestMapping("/ses")
@RequiredArgsConstructor
public class EmailController {

    private final SESMailService SESMailService;

    /**
     * ------------------------------------------
     * 텍스트 이메일 전송
     * ------------------------------------------
     */
    @PostMapping("/text-mail")
    public ResponseEntity<ResponseBasicEmailDTO> sendEmail(@Valid @RequestBody RequestBasicEmailDto requestBasicEmailDTO) {

        String messageId = SESMailService.sendEmail(requestBasicEmailDTO);

        ResponseBasicEmailDTO responseBasicEmailDTO = new ResponseBasicEmailDTO();
        responseBasicEmailDTO.setMessageId(messageId);

        return new ResponseEntity<>(responseBasicEmailDTO, HttpStatus.OK);
    }

    /**
     * ------------------------------------------
     * 탬플릿 이메일 전송
     * SendTemplatedEmailResult sendTemplatedEmail(SendTemplatedEmailRequest sendTemplatedEmailRequest);
     * ------------------------------------------
     */
    @PostMapping("/templated-mail")
    public ResponseEntity<ResponseTemplatedEmailDTO> sendTemplatedEmail(@Valid @RequestBody RequestTemplatedEmailDto requestTemplatedEmailDto) {
        String messageId = SESMailService.sendTemplatedEmail(requestTemplatedEmailDto);

        ResponseTemplatedEmailDTO responseTemplatedEmailDTO = new ResponseTemplatedEmailDTO();
        responseTemplatedEmailDTO.setMessageId(messageId);

        return new ResponseEntity<>(responseTemplatedEmailDTO, HttpStatus.OK);
    }

    /**
     * ------------------------------------------
     * 탬플릿 등록
     * CreateTemplateResult createTemplate(CreateTemplateRequest createTemplateRequest);
     * ------------------------------------------
     */
    @PostMapping("/template")
    public ResponseEntity<ResponseTemplatedDTO> createTemplate(@Valid @RequestBody RequestTemplateDto requestTemplateDto) {
        String awsRequestId = SESMailService.createTemplateEmail(requestTemplateDto);

        ResponseTemplatedDTO responseTemplatedDTO = new ResponseTemplatedDTO();
        responseTemplatedDTO.setAwsRequestId(awsRequestId);

        return new ResponseEntity<>(responseTemplatedDTO, HttpStatus.OK);
    }

    /**
     * ------------------------------------------
     * 탬플릿 변경
     * UpdateTemplateResult updateTemplate(UpdateTemplateRequest updateTemplateRequest);
     * ------------------------------------------
     */
    @PatchMapping("/template")
    public ResponseEntity<ResponseTemplatedDTO> updateTemplate(@Valid @RequestBody RequestTemplateDto requestTemplateDto) {
        String awsRequestId = SESMailService.updateTemplateEmail(requestTemplateDto);

        ResponseTemplatedDTO responseTemplatedDTO = new ResponseTemplatedDTO();
        responseTemplatedDTO.setAwsRequestId(awsRequestId);

        return new ResponseEntity<>(responseTemplatedDTO, HttpStatus.OK);
    }

    /**
     * ------------------------------------------
     * 탬플릿 삭제
     * DeleteTemplateResult deleteTemplate(DeleteTemplateRequest deleteTemplateRequest);
     * ------------------------------------------
     */
    @DeleteMapping("/template")
    public ResponseEntity<ResponseDeleteTemplatedDTO> deleteTemplate(@Valid @RequestBody RequestDeleteTemplateDto requestDeleteTemplateDto) {
        String awsRequestId = SESMailService.deleteTemplate(requestDeleteTemplateDto);

        ResponseDeleteTemplatedDTO responseDeleteTemplatedDTO = new ResponseDeleteTemplatedDTO();
        responseDeleteTemplatedDTO.setAwsRequestId(awsRequestId);

        return new ResponseEntity<>(responseDeleteTemplatedDTO, HttpStatus.OK);
    }

    /**
     * ------------------------------------------
     * 탬플릿 목록 가져오기
     * ListTemplatesResult listTemplates(ListTemplatesRequest listTemplatesRequest);
     * ------------------------------------------
     */
    @GetMapping("/templates")
    public ResponseEntity<List<TemplateMetadata>> listTemplate() {
        return new ResponseEntity<List<TemplateMetadata>>(SESMailService.listTemplates(), HttpStatus.OK);
    }

}
