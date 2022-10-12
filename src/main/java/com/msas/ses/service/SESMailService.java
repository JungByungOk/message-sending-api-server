package com.msas.ses.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.msas.ses.dto.EmailDto;
import com.msas.ses.exception.AwsSesClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
public class SESMailService {
    private static final String CHAR_SET = "UTF-8";

    private final AmazonSimpleEmailService emailService;

    @Autowired
    public SESMailService(AmazonSimpleEmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * AWS SES 의 SendEmail API 를 이용하여 기본 메일 전송 메서드
     */
    public String sendEmail(EmailDto emailDto) {

        SendEmailResult sendEmailResult;

        try {
            // The time for request/response round trip to aws in milliseconds
            int requestTimeout = 3000;

            SendEmailRequest request = new SendEmailRequest()
                    .withSource(emailDto.getFromEmail())
                    .withDestination(
                            new Destination().withToAddresses(emailDto.getToEmail()))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset(CHAR_SET)
                                            .withData(emailDto.getBody())))
                            .withSubject(new Content()
                                    .withCharset(CHAR_SET)
                                    .withData(emailDto.getSubject())))
                    .withSdkRequestTimeout(requestTimeout);

            sendEmailResult = emailService.sendEmail(request);

        } catch (AmazonSimpleEmailServiceException ex) {
            throw new AwsSesClientException("이메일 전송 실패", ex);
        }

        System.out.printf("%s\n", sendEmailResult.toString());
        return sendEmailResult.toString();
    }

}
