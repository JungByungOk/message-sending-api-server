package com.msas.pollingchecker.service;

import com.google.gson.Gson;
import com.msas.pollingchecker.model.NewEmailEntity;
import com.msas.pollingchecker.repository.EmailSendRepository;
import com.msas.pollingchecker.types.EnumEmailSendStatusCode;
import com.msas.ses.service.EmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueuedEmailPoller {

    @Value("${spring.application.name}")
    String serverName;

    private final EmailSendRepository emailSendRepository;
    private final EmailDispatchService emailDispatchService;

    @Transactional
    @Scheduled(fixedRateString = "${polling.schedule.send-email-check-time:10000}", initialDelay = 20000)
    public void checkNewEmailTask() {

        log.info("[PollingChecker] 신규 발송 이메일 확인 중...");

        List<NewEmailEntity> newEmailEntities = emailSendRepository.findNewEmail();

        if (newEmailEntities.isEmpty())
            return;

        AtomicInteger nSendingEmails = new AtomicInteger();
        newEmailEntities.forEach(newEmailEntity -> {
            nSendingEmails.addAndGet(newEmailEntity.getNewEmailDetailEntities().size());
        });
        log.info("[PollingChecker] 신규 이메일 발견. (건수: {})", nSendingEmails);

        newEmailEntities.forEach(newEmailEntity -> {

            if (newEmailEntity.getNewEmailDetailEntities() == null || newEmailEntity.getNewEmailDetailEntities().isEmpty()) {
                log.warn("[PollingChecker] 상세 목록이 비어 있어 건너뜀. (email_send_seq: {})", newEmailEntity.getEmail_send_seq());
                return;
            }

            // 상태를 Queued로 변경하여 다음 폴링에서 재조회 방지
            updateSendEmailStatus(newEmailEntity);

            // EmailDispatchService를 통해 직접 enqueue
            try {
                dispatchNewEmail(newEmailEntity);
            } catch (Exception e) {
                log.error("[PollingChecker] 이메일 dispatch 실패. (email_send_seq: {})", newEmailEntity.getEmail_send_seq(), e);
            }
        });
    }

    private void updateSendEmailStatus(NewEmailEntity newEmailEntity) {
        newEmailEntity.getNewEmailDetailEntities().forEach(newEmailDetailEntity -> {
            emailSendRepository.UpdateSendEmailStatus2Scheduler(
                    newEmailDetailEntity.getEmail_send_dtl_seq(),
                    EnumEmailSendStatusCode.Queued.name(),
                    serverName
            );
            log.info("[PollingChecker] 이메일 상태 변경. (-> Queued, dtlSeq: {})",
                    newEmailDetailEntity.getEmail_send_dtl_seq());
        });
    }

    private void dispatchNewEmail(NewEmailEntity newEmailEntity) {
        Gson gson = new Gson();
        String templateName = newEmailEntity.getNewEmailDetailEntities().get(0).getEmail_tmplet_id();
        String from = newEmailEntity.getNewEmailDetailEntities().get(0).getSend_email_addr();
        String tenantId = newEmailEntity.getTenant_id();

        List<EmailDispatchService.Recipient> recipients = new ArrayList<>();
        newEmailEntity.getNewEmailDetailEntities().forEach(detail -> {
            Map<String, String> templateData = new HashMap<>();
            String emailCts = detail.getEmail_cts();
            if (emailCts != null && emailCts.trim().startsWith("{")) {
                try {
                    templateData = gson.fromJson(emailCts, HashMap.class);
                } catch (Exception e) {
                    log.warn("[PollingChecker] 템플릿 파라미터 파싱 실패. (dtlSeq: {})",
                            detail.getEmail_send_dtl_seq());
                }
            }
            recipients.add(new EmailDispatchService.Recipient(detail.getRcv_email_addr(), templateData));
        });

        emailDispatchService.dispatch(new EmailDispatchService.DispatchRequest(
                tenantId,
                from,
                templateName,
                null,
                null,
                recipients
        ));

        log.info("[PollingChecker] 이메일 dispatch 완료. (email_send_seq: {}, 수신자: {}건)",
                newEmailEntity.getEmail_send_seq(), recipients.size());
    }
}
