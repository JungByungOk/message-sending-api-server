package com.msas.callback;

import com.msas.pollingchecker.repository.SESMariaDBRepository;
import com.msas.suppression.SuppressionEntity;
import com.msas.suppression.SuppressionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SESCallbackService {

    private static final String STATUS_DELIVERY  = "SD";
    private static final String STATUS_BOUNCE    = "SB";
    private static final String STATUS_COMPLAINT = "SC";
    private static final String SERVER_NAME      = "callback-service";

    private final SESMariaDBRepository sesMariaDBRepository;
    private final SuppressionService suppressionService;

    private volatile LocalDateTime lastEventTime;

    /**
     * SES 이벤트를 처리합니다.
     * DELIVERY → 상태 SD
     * BOUNCE   → 상태 SB + 수신 거부 목록 추가
     * COMPLAINT → 상태 SC + 수신 거부 목록 추가
     */
    public CallbackResponseDTO processEvent(SESCallbackEventDTO event) {
        lastEventTime = LocalDateTime.now();
        log.info("SESCallbackService - 이벤트 수신. (messageId: {}, eventType: {})",
                event.getMessageId(), event.getEventType());

        String statusCode;
        switch (event.getEventType()) {
            case "DELIVERY":
                statusCode = STATUS_DELIVERY;
                sesMariaDBRepository.UpdateFinalEmailStatus(
                        event.getMessageId(), statusCode, event.getEventType(), SERVER_NAME);
                break;

            case "BOUNCE":
                statusCode = STATUS_BOUNCE;
                sesMariaDBRepository.UpdateFinalEmailStatus(
                        event.getMessageId(), statusCode, event.getEventType(), SERVER_NAME);
                addToSuppression(event, "BOUNCE");
                break;

            case "COMPLAINT":
                statusCode = STATUS_COMPLAINT;
                sesMariaDBRepository.UpdateFinalEmailStatus(
                        event.getMessageId(), statusCode, event.getEventType(), SERVER_NAME);
                addToSuppression(event, "COMPLAINT");
                break;

            default:
                log.warn("SESCallbackService - 알 수 없는 이벤트 타입: {}", event.getEventType());
                return new CallbackResponseDTO(false, 0);
        }

        return new CallbackResponseDTO(true, 1);
    }

    public LocalDateTime getLastEventTime() {
        return lastEventTime;
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
                log.warn("SESCallbackService - 수신 거부 목록 추가 실패. (email: {})", email, e);
            }
        }
    }
}
