package com.msas.scheduler.job;

import com.google.common.util.concurrent.RateLimiter;

import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.ses.dto.EmailSendDetailInsertDTO;
import com.msas.ses.dto.EmailSendMasterInsertDTO;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * API Controller 통해서 들어온
 * 이메일 전송 예약일 경우에 사용하는 작업 처리기
 */
@Component
@Slf4j
@DisallowConcurrentExecution
public class SendTemplatedEmailJob extends AbstractSendTemplatedEmailJob {

    private final RateLimiter rateLimiter = RateLimiter.create(50.0);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        deleteJobIfNotInterrupted(context);

        RequestTemplatedEmailScheduleJobDTO dto = deserializeJobData(context);
        int total = dto.getTemplatedEmailList().size();

        log.info("[SendTemplatedEmailJob] 작업 시작. (jobKey: {}, 총 건수: {})", jobKey, total);

        // 발송 결과 마스터 레코드 생성
        EmailSendMasterInsertDTO master = new EmailSendMasterInsertDTO();
        master.setEmailTypCd("TEMPLATE");
        master.setSendDivCd("S"); // S = Scheduled
        master.setTenantId(dto.getTenantId());
        emailResultRepository.insertEmailSendMaster(master);

        // subject 결정: 매핑 테이블 > templateName
        String emailSubject = templateTenantRepository.selectSubjectByTemplate(dto.getTemplateName());
        if (emailSubject == null || emailSubject.isEmpty()) {
            emailSubject = dto.getTemplateName();
        }

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < total; i++) {
            rateLimiter.acquire();

            RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto emailItem = dto.getTemplatedEmailList().get(i);
            String correlationId = emailItem.getCorrelationId();
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
                emailItem.setCorrelationId(correlationId);
            }

            // 발송 결과 상세 레코드 생성
            EmailSendDetailInsertDTO detail = new EmailSendDetailInsertDTO();
            detail.setEmailSendSeq(master.getEmailSendSeq());
            detail.setSendStsCd("Queued");
            detail.setCorrelationId(correlationId);
            detail.setRcvEmailAddr(String.join(",", emailItem.getTo()));
            detail.setSendEmailAddr(dto.getFrom());
            detail.setEmailTitle(emailSubject);
            detail.setEmailTmpletId(dto.getTemplateName());
            detail.setTenantId(dto.getTenantId());
            emailResultRepository.insertEmailSendDetail(detail);

            try {
                String messageId = sendTemplatedEmail(getTemplatedEmailDto(dto, i), correlationId, dto.getTenantId());

                emailResultRepository.updateEmailSendDetailAfterSend(
                        detail.getEmailSendDtlSeq(), "Sending", correlationId, messageId);

                successCount++;
                if ((i + 1) % 100 == 0 || i + 1 == total) {
                    log.info("[SendTemplatedEmailJob] 발송 진행중. ({}/{}, 성공: {}, 실패: {}, template: {})",
                            i + 1, total, successCount, failCount, dto.getTemplateName());
                }
            } catch (Exception e) {
                failCount++;
                emailResultRepository.updateEmailSendDetailAfterSend(
                        detail.getEmailSendDtlSeq(), "Error", correlationId, null);
                log.error("[SendTemplatedEmailJob] 이메일 발송 실패. ({}/{}, correlationId: {}, message: {})",
                        i + 1, total, correlationId, e.getMessage());
            }
        }

        completeBatch(context, failCount > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");

        log.info("[SendTemplatedEmailJob] 작업 종료. (jobKey: {}, 성공: {}, 실패: {}, 총: {})",
                jobKey, successCount, failCount, total);
    }
}
