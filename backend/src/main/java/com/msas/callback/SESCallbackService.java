package com.msas.callback;

import com.msas.pollingchecker.repository.EmailSendRepository;
import com.msas.suppression.SuppressionEntity;
import com.msas.suppression.SuppressionService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SESCallbackService {

    private static final String STATUS_DELIVERED  = "Delivered";
    private static final String STATUS_BOUNCED    = "Bounced";
    private static final String STATUS_COMPLAINED = "Complained";
    private static final String STATUS_REJECTED   = "Rejected";
    private static final String STATUS_DELAYED    = "Delayed";
    private static final String STATUS_ERROR      = "Error";
    private static final String SERVER_NAME      = "callback-service";

    private final EmailSendRepository emailSendRepository;
    private final SuppressionService suppressionService;
    private final Gson gson = new Gson();

    private volatile LocalDateTime lastEventTime;

    /**
     * SES 이벤트를 처리합니다.
     * DELIVERY          → Delivered
     * BOUNCE            → Bounced + 수신 거부 목록 추가
     * COMPLAINT         → Complained + 수신 거부 목록 추가
     * OPEN / CLICK      → Delivered 유지 (Engagement 이벤트, 멱등)
     * REJECT            → Rejected
     * DELIVERY_DELAY    → Delayed
     * RENDERING_FAILURE → Error
     */
    public CallbackResponseDTO processEvent(SESCallbackEventDTO event) {
        lastEventTime = LocalDateTime.now();
        log.info("[SESCallbackService] 이벤트 수신. (messageId: {}, eventType: {}, tenantId: {})",
                event.getMessageId(), event.getEventType(), event.getTenantId());

        String statusCode;
        switch (event.getEventType()) {
            case "SEND":
                // Send 이벤트 - 이미 Sending 상태이므로 결과 유형만 업데이트
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), "Sending", event.getEventType(), event.getMessageId(), SERVER_NAME);
                break;

            case "DELIVERY":
                statusCode = STATUS_DELIVERED;
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), statusCode, event.getEventType(), event.getMessageId(), SERVER_NAME);
                break;

            case "BOUNCE":
                statusCode = STATUS_BOUNCED;
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), statusCode, event.getEventType(), event.getMessageId(), SERVER_NAME);
                addToSuppression(event, "BOUNCE");
                break;

            case "COMPLAINT":
                statusCode = STATUS_COMPLAINED;
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), statusCode, event.getEventType(), event.getMessageId(), SERVER_NAME);
                addToSuppression(event, "COMPLAINT");
                break;

            case "OPEN":
            case "CLICK":
                // Engagement 이벤트 - send_sts_cd 변경 없이 결과 유형만 업데이트 (Delivered 유지, 멱등)
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), STATUS_DELIVERED, event.getEventType(), event.getMessageId(), SERVER_NAME);
                log.debug("[SESCallbackService] {} Engagement 이벤트 처리 완료. (correlationId: {}, messageId: {})",
                        event.getEventType(), event.getCorrelationId(), event.getMessageId());
                break;

            case "REJECT":
                statusCode = STATUS_REJECTED;
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), statusCode, event.getEventType(), event.getMessageId(), SERVER_NAME);
                break;

            case "DELIVERY_DELAY":
                statusCode = STATUS_DELAYED;
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), statusCode, event.getEventType(), event.getMessageId(), SERVER_NAME);
                break;

            case "RENDERING_FAILURE":
                statusCode = STATUS_ERROR;
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), statusCode, event.getEventType(), event.getMessageId(), SERVER_NAME);
                break;

            case "SUBSCRIPTION":
                // 구독 이벤트 - Delivered 유지 (멱등)
                emailSendRepository.UpdateFinalEmailStatus(
                        event.getCorrelationId(), STATUS_DELIVERED, event.getEventType(), event.getMessageId(), SERVER_NAME);
                break;

            default:
                log.warn("[SESCallbackService] 알 수 없는 이벤트 타입. (eventType: {}, messageId: {})",
                        event.getEventType(), event.getMessageId());
                return new CallbackResponseDTO(false, 0);
        }

        // 모든 이벤트를 이력 테이블에 기록
        logEvent(event);

        return new CallbackResponseDTO(true, 1);
    }

    public LocalDateTime getLastEventTime() {
        return lastEventTime;
    }

    private void logEvent(SESCallbackEventDTO event) {
        try {
            String recipients = event.getRecipients() != null ? gson.toJson(event.getRecipients()) : null;
            String extraData = event.getDetails() != null && !event.getDetails().isEmpty() ? gson.toJson(event.getDetails()) : null;
            emailSendRepository.InsertEmailEventLog(
                    event.getCorrelationId(),
                    event.getMessageId(),
                    event.getTenantId(),
                    event.getEventType(),
                    recipients,
                    extraData,
                    SERVER_NAME
            );
        } catch (Exception e) {
            log.warn("[SESCallbackService] 이벤트 이력 저장 실패. (messageId: {}, eventType: {})",
                    event.getMessageId(), event.getEventType(), e);
        }
    }

    private void addToSuppression(SESCallbackEventDTO event, String reason) {
        if (event.getRecipients() == null) return;
        for (String email : event.getRecipients()) {
            try {
                SuppressionEntity entity = new SuppressionEntity();
                entity.setTenantId(event.getTenantId());
                entity.setEmail(email);
                entity.setReason(reason);
                suppressionService.addSuppression(entity);
            } catch (Exception e) {
                log.warn("[SESCallbackService] 수신 거부 목록 추가 실패. (email: {})", email, e);
            }
        }
    }
}
