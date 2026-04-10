package com.msas.ses.service;

import com.google.gson.Gson;
import com.msas.ses.dto.EmailSendDetailInsertDTO;
import com.msas.ses.dto.EmailSendMasterInsertDTO;
import com.msas.ses.repository.EmailResultRepository;
import com.msas.settings.service.ApiGatewayClient;
import com.msas.tenant.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailDispatchService {

    private final ApiGatewayClient apiGatewayClient;
    private final EmailResultRepository emailResultRepository;
    private final QuotaService quotaService;
    private final Gson gson = new Gson();

    /**
     * 이메일 발송 요청을 API Gateway /email-enqueue로 전달합니다.
     * enqueue Lambda가 수신자별 SQS 메시지를 생성합니다.
     */
    public Map<String, Object> dispatch(DispatchRequest request) {
        String tenantId = request.tenantId();

        // 1. 할당량 검증
        quotaService.checkQuota(tenantId, request.recipients().size());

        // 2. DB에 발송 기록 생성
        EmailSendMasterInsertDTO master = new EmailSendMasterInsertDTO();
        master.setEmailTypCd(request.templateName() != null ? "TEMPLATE" : "TEXT");
        master.setSendDivCd(request.templateName() != null ? "P" : "H");
        master.setTenantId(tenantId);
        emailResultRepository.insertEmailSendMaster(master);

        // 3. 수신자별 상세 기록 생성
        List<Map<String, Object>> recipientList = new ArrayList<>();
        for (var recipient : request.recipients()) {
            String correlationId = UUID.randomUUID().toString();

            EmailSendDetailInsertDTO detail = new EmailSendDetailInsertDTO();
            detail.setEmailSendSeq(master.getEmailSendSeq());
            detail.setSendStsCd("Queued");
            detail.setCorrelationId(correlationId);
            detail.setRcvEmailAddr(recipient.email());
            detail.setSendEmailAddr(request.from());
            detail.setEmailTitle(request.subject() != null ? request.subject() : request.templateName());
            detail.setEmailTmpletId(request.templateName());
            detail.setTenantId(tenantId);
            emailResultRepository.insertEmailSendDetail(detail);

            Map<String, Object> recipientMsg = new LinkedHashMap<>();
            recipientMsg.put("to", recipient.email());
            recipientMsg.put("correlationId", correlationId);
            recipientMsg.put("emailSendSeq", master.getEmailSendSeq());
            recipientMsg.put("emailSendDtlSeq", detail.getEmailSendDtlSeq());
            if (recipient.templateData() != null) {
                recipientMsg.put("templateData", recipient.templateData());
            }
            recipientList.add(recipientMsg);
        }

        // 4. API Gateway /email-enqueue 호출
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("from", request.from());
        if (request.templateName() != null) {
            payload.put("templateName", request.templateName());
        }
        if (request.subject() != null) {
            payload.put("subject", request.subject());
        }
        if (request.htmlBody() != null) {
            payload.put("htmlBody", request.htmlBody());
        }
        payload.put("recipients", recipientList);

        try {
            String jsonBody = gson.toJson(payload);
            HttpResponse<String> response = apiGatewayClient.post("/email-enqueue", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = gson.fromJson(response.body(),
                        new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
                log.info("[EmailDispatchService] 발송 큐 등록 완료. (tenantId: {}, 수신자: {}건, enqueued: {})",
                        tenantId, recipientList.size(), result.get("enqueued"));
                result.put("emailSendSeq", master.getEmailSendSeq());
                return result;
            } else {
                log.error("[EmailDispatchService] enqueue 실패. (HTTP {}, body: {})", response.statusCode(), response.body());
                throw new RuntimeException("이메일 발송 큐 등록 실패 (HTTP " + response.statusCode() + ")");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EmailDispatchService] enqueue 호출 실패.", e);
            throw new RuntimeException("이메일 발송 큐 등록 실패: " + e.getMessage());
        }
    }

    public record Recipient(String email, Map<String, String> templateData) {}

    public record DispatchRequest(
            String tenantId,
            String from,
            String templateName,
            String subject,
            String htmlBody,
            List<Recipient> recipients
    ) {}
}
