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
public class PollingNewEmailFromNFTDB {

    @Value("${spring.application.name}")
    String serverName;

    private final EmailSendRepository emailSendRepository;
    private final EmailDispatchService emailDispatchService;

    @Transactional
    @Scheduled(fixedRateString = "${polling.schedule.send-email-check-time:10000}", initialDelay = 20000)
    public void checkNewEmailTask() {

        log.info("@RDBMS Checking - Are there new email items to send?");

        List<NewEmailEntity> newEmailEntities = emailSendRepository.findNewEmail();

        if (newEmailEntities.isEmpty())
            return;

        AtomicInteger nSendingEmails = new AtomicInteger();
        newEmailEntities.forEach(newEmailEntity -> {
            nSendingEmails.addAndGet(newEmailEntity.getNewEmailDetailEntities().size());
        });
        log.info("@RDBMS Checking - New registered email [ {} ]", nSendingEmails);

        newEmailEntities.forEach(newEmailEntity -> {

            if (newEmailEntity.getNewEmailDetailEntities() == null || newEmailEntity.getNewEmailDetailEntities().isEmpty()) {
                log.warn("@RDBMS Checking - Skipping email_send_seq={} with empty detail list", newEmailEntity.getEmail_send_seq());
                return;
            }

            UpdateSendEmailStatus(newEmailEntity);

            try {
                dispatchNewEmail(newEmailEntity);
            } catch (Exception e) {
                log.error("@RDBMS Checking - dispatch 실패. (email_send_seq: {})", newEmailEntity.getEmail_send_seq(), e);
            }
        });
    }

    private void UpdateSendEmailStatus(NewEmailEntity newEmailEntity) {
        newEmailEntity.getNewEmailDetailEntities().forEach(newEmailDetailEntity -> {
            emailSendRepository.UpdateSendEmailStatus2Scheduler(
                    newEmailDetailEntity.getEmail_send_dtl_seq(),
                    EnumEmailSendStatusCode.SQ.name(),
                    serverName
            );
            log.info("@RDBMS Checking - Email status update with [ SR->SQ ].");
        });
    }

    private void dispatchNewEmail(NewEmailEntity newEmailEntity) {
        Gson gson = new Gson();
        String templateName = newEmailEntity.getNewEmailDetailEntities().get(0).getEmail_tmplet_id();
        String from = newEmailEntity.getNewEmailDetailEntities().get(0).getSend_email_addr();

        List<EmailDispatchService.Recipient> recipients = new ArrayList<>();
        newEmailEntity.getNewEmailDetailEntities().forEach(detail -> {
            Map<String, String> templateData = new HashMap<>();
            String emailCts = detail.getEmail_cts();
            if (emailCts != null && !emailCts.isBlank()) {
                try {
                    templateData = gson.fromJson(emailCts, HashMap.class);
                } catch (Exception e) {
                    log.warn("@RDBMS Checking - 템플릿 파라미터 파싱 실패. (dtlSeq: {})", detail.getEmail_send_dtl_seq());
                }
            }
            recipients.add(new EmailDispatchService.Recipient(detail.getRcv_email_addr(), templateData));
        });

        emailDispatchService.dispatch(new EmailDispatchService.DispatchRequest(
                null,
                from,
                templateName,
                null,
                null,
                recipients
        ));

        log.info("@RDBMS Checking - dispatch 완료. (email_send_seq: {}, 수신자: {}건)",
                newEmailEntity.getEmail_send_seq(), recipients.size());
    }
}
