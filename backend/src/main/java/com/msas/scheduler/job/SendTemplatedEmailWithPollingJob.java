package com.msas.scheduler.job;

import com.google.common.util.concurrent.RateLimiter;
import com.msas.common.utils.ForeachUtils;
import com.msas.pollingchecker.repository.SESMariaDBRepository;
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

    private final RateLimiter rateLimiter = RateLimiter.create(7.0);

    @Value("${spring.application.name}")
    private String serverName;

    private SESMariaDBRepository sesMariaDBRepository;

    @Autowired
    public void setSESMariaDBRepository(SESMariaDBRepository sesMariaDBRepository) {
        this.sesMariaDBRepository = sesMariaDBRepository;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        deleteJobIfNotInterrupted(context);

        log.info("@SendTemplatedEmailWithPollingJob - started :: jobKey={}", jobKey);

        RequestTemplatedEmailScheduleJobDTO dto = deserializeJobData(context);

        dto.getTemplatedEmailList().forEach(ForeachUtils.withCounter((count, templatedEmail) -> {
            int emailSendDtlSeq = Integer.parseInt(dto.getTemplatedEmailList().get(count).getId());
            EnumEmailSendStatusCode successCode = EnumEmailSendStatusCode.SM;
            EnumSESEventTypeCode failCode = EnumSESEventTypeCode.SESFail;

            try {
                rateLimiter.acquire();

                String messageId = sendTemplatedEmail(getTemplatedEmailDto(dto, count));

                log.info("@SendTemplatedEmailWithPollingJob - Email sending ({}/{}) : templateName = {}, messageId = {}",
                        count + 1, dto.getTemplatedEmailList().size(), dto.getTemplateName(), messageId);

                updateSendEmailStatus(emailSendDtlSeq, successCode.name().toUpperCase(Locale.ENGLISH), null, messageId);

            } catch (Exception e) {
                log.error("@SendTemplatedEmailWithPollingJob - {}", e.getMessage());

                updateSendEmailStatus(emailSendDtlSeq,
                        failCode.getEmailSendStatusCode().name(),
                        failCode.name().toUpperCase(Locale.ENGLISH),
                        null);
            }
        }));

        log.info("@SendTemplatedEmailWithPollingJob - ended :: jobKey={}", jobKey);
    }

    private void updateSendEmailStatus(int emailSendDtlSeq, String sendStsCd, String sendRsltTypCd, String messageId) {
        sesMariaDBRepository.UpdateSendEmailStatus2AWSSES(emailSendDtlSeq, sendStsCd, sendRsltTypCd, messageId, serverName);
        log.info("@SendTemplatedEmailWithPollingJob - Email status update with [ SQ->{} ].", sendStsCd);
    }
}
