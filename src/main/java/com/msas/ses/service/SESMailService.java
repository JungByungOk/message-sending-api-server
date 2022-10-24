package com.msas.ses.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.google.gson.Gson;
import com.msas.ses.dto.RequestBasicEmailDto;
import com.msas.ses.dto.RequestDeleteTemplateDto;
import com.msas.ses.dto.RequestTemplateDto;
import com.msas.ses.dto.RequestTemplatedEmailDto;
import com.msas.ses.exception.AwsSesClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SESMailService {
    private static final String CHAR_SET = "UTF-8";

    private final AmazonSimpleEmailService amazonSimpleEmailService;

    @Autowired
    public SESMailService(AmazonSimpleEmailService amazonSimpleEmailService) {
        this.amazonSimpleEmailService = amazonSimpleEmailService;
    }

    /**
     * AWS SES 의 SendEmail API 를 이용하여 기본 메일 전송 메서드
     */
    public String sendEmail(RequestBasicEmailDto requestBasicEmailDto) {

        SendEmailResult sendEmailResult;

        try {
            // The time for request/response round trip to aws in milliseconds
            int requestTimeout = 3000;

            SendEmailRequest request = new SendEmailRequest()
                    .withSource(requestBasicEmailDto.getFrom())
                    .withDestination(
                            new Destination().withToAddresses(requestBasicEmailDto.getTo()))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset(CHAR_SET)
                                            .withData(requestBasicEmailDto.getBody())))
                            .withSubject(new Content()
                                    .withCharset(CHAR_SET)
                                    .withData(requestBasicEmailDto.getSubject())))
                    .withSdkRequestTimeout(requestTimeout);

            request.setTags(requestBasicEmailDto.getTags());

            sendEmailResult = amazonSimpleEmailService.sendEmail(request);

        } catch (AmazonSimpleEmailServiceException ex) {
            log.debug("이메일 전송 실패", ex);
            throw new AwsSesClientException("이메일 전송 실패", ex); // to GlobalControllerAdvice
        }

        String emailMessageId = sendEmailResult.getMessageId();
        log.debug("이메일 전송 완료 (messageId: {})", emailMessageId);

        return emailMessageId;
    }

    public String createTemplateEmail(RequestTemplateDto requestTemplateDto) {

        CreateTemplateResult createTemplateResult;

        try {
            Template template = new Template()
                    .withTemplateName(requestTemplateDto.getTemplateName())
                    .withSubjectPart(requestTemplateDto.getSubjectPart())
                    .withHtmlPart(requestTemplateDto.getHtmlPart())
                    .withTextPart(requestTemplateDto.getTextPart());

            CreateTemplateRequest request = new CreateTemplateRequest().withTemplate(template);

            createTemplateResult = amazonSimpleEmailService.createTemplate(request);

        } catch (AmazonSimpleEmailServiceException ex) {
            log.debug("탬플릿 등록 실패", ex);
            throw new AwsSesClientException("탬플릿 등록 실패", ex); // to GlobalControllerAdvice
        }

        String awsRequestId = createTemplateResult.getSdkResponseMetadata().getRequestId();
        log.debug("탬플릿 등록 완료 (AWS_REQUEST_ID: {})", awsRequestId);

        return awsRequestId;
    }

    public String sendTemplatedEmail(RequestTemplatedEmailDto requestTemplatedEmailDto) {

        SendTemplatedEmailResult sendTemplatedEmailResult;

        try {
            Destination destination = new Destination();
            {
                destination.setToAddresses(requestTemplatedEmailDto.getTo());
                destination.setCcAddresses(requestTemplatedEmailDto.getCc());
                destination.setBccAddresses(requestTemplatedEmailDto.getBcc());
            }

            SendTemplatedEmailRequest emailRequest = new SendTemplatedEmailRequest();
            {
                emailRequest.setTemplate(requestTemplatedEmailDto.getTemplateName());
                emailRequest.setDestination(destination);
                emailRequest.setSource(requestTemplatedEmailDto.getFrom());
                emailRequest.setTemplateData(new Gson().toJson(requestTemplatedEmailDto.getTemplateData()));
                emailRequest.setTags(requestTemplatedEmailDto.getTags());   // custom tag: 발송 단위 별로 이벤트 로그 추적 정보
            }

            sendTemplatedEmailResult = amazonSimpleEmailService.sendTemplatedEmail(emailRequest);
        } catch (AmazonSimpleEmailServiceException ex) {
            log.debug("탬플릿 메일 전송 실패", ex);
            throw new AwsSesClientException("탬플릿 메일 전송 실패", ex); // to GlobalControllerAdvice
        }

        String messageId = sendTemplatedEmailResult.getMessageId();
        log.debug("탬플릿 메일 전송 성공 (MessageId: {})", messageId);

        return messageId;
    }

    public String updateTemplateEmail(RequestTemplateDto requestTemplateDto) {
        UpdateTemplateResult updateTemplateResult;

        try {
            Template template = new Template()
                    .withTemplateName(requestTemplateDto.getTemplateName())
                    .withSubjectPart(requestTemplateDto.getSubjectPart())
                    .withHtmlPart(requestTemplateDto.getHtmlPart())
                    .withTextPart(requestTemplateDto.getTextPart());

            UpdateTemplateRequest updateTemplateRequest = new UpdateTemplateRequest();
            updateTemplateRequest.setTemplate(template);

            updateTemplateResult = amazonSimpleEmailService.updateTemplate(updateTemplateRequest);
        } catch (AmazonSimpleEmailServiceException ex) {
            log.debug("탬플릿 수정 실패", ex);
            throw new AwsSesClientException("탬플릿 수정 실패", ex); // to GlobalControllerAdvice
        }

        String awsRequestId = updateTemplateResult.getSdkResponseMetadata().getRequestId();
        log.debug("탬플릿 수정 실패 (AWS_REQUEST_ID: {})", awsRequestId);

        return awsRequestId;
    }

    public String deleteTemplate(RequestDeleteTemplateDto requestDeleteTemplateDto) {
        DeleteTemplateResult deleteTemplateResult;

        try {
            DeleteTemplateRequest deleteTemplateRequest = new DeleteTemplateRequest();
            deleteTemplateRequest.setTemplateName(requestDeleteTemplateDto.getTemplateName());

            deleteTemplateResult = amazonSimpleEmailService.deleteTemplate(deleteTemplateRequest);
        } catch (AmazonSimpleEmailServiceException ex) {
            log.debug("탬플릿 삭제 실패", ex);
            throw new AwsSesClientException("탬플릿 삭제 실패", ex); // to GlobalControllerAdvice
        }

        String awsRequestId = deleteTemplateResult.getSdkResponseMetadata().getRequestId();
        log.debug("탬플릿 수정 실패 (AWS_REQUEST_ID: {})", awsRequestId);

        return awsRequestId;

    }

    public List<TemplateMetadata> listTemplates() {

        ListTemplatesResult listTemplatesResult;
        List<TemplateMetadata> listTotalTemplates = new ArrayList<>();

        try {

            do {

                ListTemplatesRequest listTemplatesRequest = new ListTemplatesRequest();
                listTemplatesRequest.setMaxItems(10); // 최대 10개 까지

                listTemplatesResult = amazonSimpleEmailService.listTemplates(listTemplatesRequest);

                listTotalTemplates.addAll(listTemplatesResult.getTemplatesMetadata()); // 전체 목록 취합

            } while (listTemplatesResult.getNextToken() != null);

        } catch (AmazonSimpleEmailServiceException ex) {
            log.debug("탬플릿 목록 가져오기 실패", ex);
            throw new AwsSesClientException("탬플릿 목록 가져오기 실패", ex); // to GlobalControllerAdvice
        }

        int templateCount = listTotalTemplates.size();
        log.debug("탬플릿 목록 가져오기 성공 (Number of registered templates : {})", templateCount);

        return listTotalTemplates;

    }

}
