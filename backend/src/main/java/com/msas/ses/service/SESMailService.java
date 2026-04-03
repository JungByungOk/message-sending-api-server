package com.msas.ses.service;

import com.google.gson.Gson;
import com.msas.ses.dto.*;
import com.msas.ses.exception.AwsSesClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SESMailService {
    private static final String CHAR_SET = "UTF-8";

    private final SesClient sesClient;

    /**
     * AWS SES 의 SendEmail API 를 이용하여 기본 메일 전송 메서드
     */
    public String sendEmail(RequestBasicEmailDto requestBasicEmailDto) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(requestBasicEmailDto.getFrom())
                    .destination(Destination.builder()
                            .toAddresses(requestBasicEmailDto.getTo())
                            .build())
                    .message(Message.builder()
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .charset(CHAR_SET)
                                            .data(requestBasicEmailDto.getBody())
                                            .build())
                                    .build())
                            .subject(Content.builder()
                                    .charset(CHAR_SET)
                                    .data(requestBasicEmailDto.getSubject())
                                    .build())
                            .build())
                    .tags(toSdkMessageTags(requestBasicEmailDto.getTags()))
                    .overrideConfiguration(c -> c.apiCallTimeout(java.time.Duration.ofMillis(3000)))
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            String emailMessageId = response.messageId();
            log.info("SESMailService - Email has been sent. (messageId: {})", emailMessageId);
            return emailMessageId;

        } catch (SesException ex) {
            log.error("SESMailService - Email sending failed.", ex);
            throw new AwsSesClientException("Email sending failed.", ex);
        }
    }

    public String createTemplateEmail(RequestTemplateDto requestTemplateDto) {
        try {
            Template template = Template.builder()
                    .templateName(requestTemplateDto.getTemplateName())
                    .subjectPart(requestTemplateDto.getSubjectPart())
                    .htmlPart(requestTemplateDto.getHtmlPart())
                    .textPart(requestTemplateDto.getTextPart())
                    .build();

            CreateTemplateResponse response = sesClient.createTemplate(
                    CreateTemplateRequest.builder().template(template).build());

            String awsRequestId = response.responseMetadata() != null ? response.responseMetadata().requestId() : "unknown";
            log.info("SESMailService - A template has been registered. (AWS_REQUEST_ID: {})", awsRequestId);
            return awsRequestId;

        } catch (SesException ex) {
            log.error("SESMailService - Template registration failed.", ex);
            throw new AwsSesClientException("SESMailService - Template registration failed.", ex);
        }
    }

    public String sendTemplatedEmail(RequestTemplatedEmailDto requestTemplatedEmailDto) {
        try {
            Destination destination = Destination.builder()
                    .toAddresses(requestTemplatedEmailDto.getTo())
                    .ccAddresses(requestTemplatedEmailDto.getCc())
                    .bccAddresses(requestTemplatedEmailDto.getBcc())
                    .build();

            SendTemplatedEmailRequest emailRequest = SendTemplatedEmailRequest.builder()
                    .template(requestTemplatedEmailDto.getTemplateName())
                    .destination(destination)
                    .source(requestTemplatedEmailDto.getFrom())
                    .templateData(new Gson().toJson(requestTemplatedEmailDto.getTemplateData()))
                    .tags(toSdkMessageTags(requestTemplatedEmailDto.getTags()))
                    .build();

            SendTemplatedEmailResponse response = sesClient.sendTemplatedEmail(emailRequest);
            String messageId = response.messageId();
            log.info("SESMailService - Template mail sending was successful. (MessageId: {})", messageId);
            return messageId;

        } catch (SesException ex) {
            log.error("SESMailService - Failed to send template mail.", ex);
            throw new AwsSesClientException("SESMailService - Failed to send template mail.", ex);
        }
    }

    public String updateTemplateEmail(RequestTemplateDto requestTemplateDto) {
        try {
            Template template = Template.builder()
                    .templateName(requestTemplateDto.getTemplateName())
                    .subjectPart(requestTemplateDto.getSubjectPart())
                    .htmlPart(requestTemplateDto.getHtmlPart())
                    .textPart(requestTemplateDto.getTextPart())
                    .build();

            UpdateTemplateResponse response = sesClient.updateTemplate(
                    UpdateTemplateRequest.builder().template(template).build());

            String awsRequestId = response.responseMetadata() != null ? response.responseMetadata().requestId() : "unknown";
            log.info("SESMailService - Template modification was successful. (AWS_REQUEST_ID: {})", awsRequestId);
            return awsRequestId;

        } catch (SesException ex) {
            log.error("SESMailService - Template modification failed.", ex);
            throw new AwsSesClientException("Template modification failed.", ex);
        }
    }

    public String deleteTemplate(RequestDeleteTemplateDto requestDeleteTemplateDto) {
        try {
            DeleteTemplateResponse response = sesClient.deleteTemplate(
                    DeleteTemplateRequest.builder()
                            .templateName(requestDeleteTemplateDto.getTemplateName())
                            .build());

            String awsRequestId = response.responseMetadata() != null ? response.responseMetadata().requestId() : "unknown";
            log.info("SESMailService - Template deletion successful. (AWS_REQUEST_ID: {})", awsRequestId);
            return awsRequestId;

        } catch (SesException ex) {
            log.error("SESMailService - Failed to delete template.", ex);
            throw new AwsSesClientException("SESMailService - Failed to delete template.", ex);
        }
    }

    public List<TemplateMetadata> listTemplates() {
        List<TemplateMetadata> listTotalTemplates = new ArrayList<>();
        String nextToken = null;

        try {
            do {
                ListTemplatesRequest request = ListTemplatesRequest.builder()
                        .maxItems(10)
                        .nextToken(nextToken)
                        .build();

                ListTemplatesResponse response = sesClient.listTemplates(request);
                listTotalTemplates.addAll(response.templatesMetadata());
                nextToken = response.nextToken();

            } while (nextToken != null);

        } catch (SesException ex) {
            log.debug("SESMailService - Failed to get template list.", ex);
            throw new AwsSesClientException("Failed to get template list.", ex);
        }

        log.debug("SESMailService - To get template list was successful. (Number of registered templates : {})", listTotalTemplates.size());
        return listTotalTemplates;
    }

    /**
     * MessageTagDto → SDK v2 MessageTag 변환
     */
    private List<MessageTag> toSdkMessageTags(List<MessageTagDto> tags) {
        if (tags == null) return null;
        return tags.stream()
                .map(t -> MessageTag.builder().name(t.getName()).value(t.getValue()).build())
                .collect(Collectors.toList());
    }
}
