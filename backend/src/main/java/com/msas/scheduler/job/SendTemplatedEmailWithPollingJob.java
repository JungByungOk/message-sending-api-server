package com.msas.scheduler.job;

import com.google.common.util.concurrent.RateLimiter;
import com.msas.pollingchecker.repository.EmailSendRepository;
import com.msas.pollingchecker.types.EnumEmailSendStatusCode;
import com.msas.pollingchecker.types.EnumSESEventTypeCode;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * PollingNewEmailFromNFTDB 서비스 통해서 들어온 이메일 전송 예약 작업 처리기
 * 이메일 등록 테이블의 상태 변경을 위한 이벤트 추가됨
 */
@Component
@Slf4j
@DisallowConcurrentExecution
public class SendTemplatedEmailWithPollingJob extends AbstractSendTemplatedEmailJob {

    private final RateLimiter rateLimiter = RateLimiter.create(50.0);

    @Value("${spring.application.name}")
    private String serverName;

    private EmailSendRepository emailSendRepository;

    @Autowired
    public void setEmailSendRepository(EmailSendRepository emailSendRepository) {
        this.emailSendRepository = emailSendRepository;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        deleteJobIfNotInterrupted(context);

        log.info("[SendTemplatedEmailWithPollingJob] 작업 시작. (jobKey: {})", jobKey);

        RequestTemplatedEmailScheduleJobDTO dto = deserializeJobData(context);

        int total = dto.getTemplatedEmailList().size();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < total; i++) {
            int emailSendDtlSeq = Integer.parseInt(dto.getTemplatedEmailList().get(i).getId());
            EnumEmailSendStatusCode successCode = EnumEmailSendStatusCode.Sending;
            EnumSESEventTypeCode failCode = EnumSESEventTypeCode.SESFail;

            try {
                rateLimiter.acquire();

                String correlationId = dto.getTemplatedEmailList().get(i).getCorrelationId();
                String messageId = sendTemplatedEmail(getTemplatedEmailDto(dto, i), correlationId, dto.getTenantId());

                successCount++;
                updateSendEmailStatus(emailSendDtlSeq, successCode.name(), null, messageId);

                if ((i + 1) % 100 == 0 || i + 1 == total) {
                    log.info("[SendTemplatedEmailWithPollingJob] 발송 진행중. ({}/{}, 성공: {}, 실패: {}, template: {})",
                            i + 1, total, successCount, failCount, dto.getTemplateName());
                }

            } catch (Exception e) {
                failCount++;
                log.error("[SendTemplatedEmailWithPollingJob] 이메일 발송 실패. (dtlSeq: {}, message: {})",
                        emailSendDtlSeq, e.getMessage());

                updateSendEmailStatus(emailSendDtlSeq,
                        failCode.getEmailSendStatusCode().name(),
                        failCode.name(),
                        null);
            }
        }

        completeBatch(context, failCount > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");

        log.info("[SendTemplatedEmailWithPollingJob] 작업 종료. (jobKey: {}, 성공: {}, 실패: {}, 총: {})",
                jobKey, successCount, failCount, total);
    }

    private void updateSendEmailStatus(int emailSendDtlSeq, String sendStsCd, String sendRsltTypCd, String messageId) {
        emailSendRepository.UpdateSendEmailStatus2AWSSES(emailSendDtlSeq, sendStsCd, sendRsltTypCd, messageId, serverName);
        log.info("[SendTemplatedEmailWithPollingJob] 이메일 상태 변경. (Queued -> {}, dtlSeq: {})", sendStsCd, emailSendDtlSeq);
    }
}
