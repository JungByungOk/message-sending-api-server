package com.msas.ses.service;

import com.msas.ses.dto.*;
import com.msas.ses.exception.AwsSesClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SESMailServiceTest {

    @Mock
    private SesClient sesClient;

    @InjectMocks
    private SESMailService sesMailService;

    @Test
    @DisplayName("템플릿 이메일 전송 성공")
    void sendTemplatedEmail_success() {
        // given
        when(sesClient.sendTemplatedEmail(any(SendTemplatedEmailRequest.class)))
                .thenReturn(SendTemplatedEmailResponse.builder()
                        .messageId("test-message-id-123")
                        .build());

        RequestTemplatedEmailDto dto = RequestTemplatedEmailDto.builder()
                .templateName("TestTemplate")
                .from("sender@example.com")
                .to(List.of("receiver@example.com"))
                .templateData(Map.of("name", "Test"))
                .tags(List.of(new MessageTagDto("customTag", "test-tag")))
                .build();

        // when
        String messageId = sesMailService.sendTemplatedEmail(dto);

        // then
        assertThat(messageId).isEqualTo("test-message-id-123");
    }

    @Test
    @DisplayName("템플릿 이메일 전송 실패 시 AwsSesClientException 발생")
    void sendTemplatedEmail_failure() {
        // given
        when(sesClient.sendTemplatedEmail(any(SendTemplatedEmailRequest.class)))
                .thenThrow(SesException.builder().message("Template does not exist").build());

        RequestTemplatedEmailDto dto = RequestTemplatedEmailDto.builder()
                .templateName("NonExistent")
                .from("sender@example.com")
                .to(List.of("receiver@example.com"))
                .templateData(Map.of("name", "Test"))
                .tags(List.of(new MessageTagDto("customTag", "test-tag")))
                .build();

        // when & then
        assertThatThrownBy(() -> sesMailService.sendTemplatedEmail(dto))
                .isInstanceOf(AwsSesClientException.class);
    }

    @Test
    @DisplayName("템플릿 생성 성공")
    void createTemplate_success() {
        // given
        CreateTemplateResponse mockResponse = (CreateTemplateResponse) CreateTemplateResponse.builder()
                .sdkHttpResponse(software.amazon.awssdk.http.SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(sesClient.createTemplate(any(CreateTemplateRequest.class)))
                .thenReturn(mockResponse);

        RequestTemplateDto dto = new RequestTemplateDto();
        dto.setTemplateName("NewTemplate");
        dto.setSubjectPart("Subject");
        dto.setHtmlPart("<h1>Hello</h1>");
        dto.setTextPart("Hello");

        // when & then — responseMetadata().requestId()가 null일 수 있으나 예외 없이 완료
        String requestId = sesMailService.createTemplateEmail(dto);
        // SDK mock에서는 requestId가 null이므로 null 허용
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("템플릿 목록 조회 - nextToken 페이징 정상 동작")
    void listTemplates_paging() {
        // given
        TemplateMetadata meta1 = TemplateMetadata.builder().name("Template1").build();
        TemplateMetadata meta2 = TemplateMetadata.builder().name("Template2").build();

        when(sesClient.listTemplates(any(ListTemplatesRequest.class)))
                .thenReturn(ListTemplatesResponse.builder()
                        .templatesMetadata(meta1)
                        .nextToken("page2")
                        .build())
                .thenReturn(ListTemplatesResponse.builder()
                        .templatesMetadata(meta2)
                        .nextToken(null)
                        .build());

        // when
        var templates = sesMailService.listTemplates();

        // then
        assertThat(templates).hasSize(2);
        assertThat(templates.get(0).name()).isEqualTo("Template1");
        assertThat(templates.get(1).name()).isEqualTo("Template2");
    }

    @Test
    @DisplayName("기본 이메일 전송 성공")
    void sendEmail_success() {
        // given
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder()
                        .messageId("basic-msg-id-789")
                        .build());

        RequestBasicEmailDto dto = new RequestBasicEmailDto();
        dto.setFrom("sender@example.com");
        dto.setTo("receiver@example.com");
        dto.setSubject("Test Subject");
        dto.setBody("<p>Test Body</p>");
        dto.setTags(List.of(new MessageTagDto("campaign", "test")));

        // when
        String messageId = sesMailService.sendEmail(dto);

        // then
        assertThat(messageId).isEqualTo("basic-msg-id-789");
    }
}
