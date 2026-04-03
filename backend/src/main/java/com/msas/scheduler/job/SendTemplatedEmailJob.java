package com.msas.scheduler.job;

import com.google.common.util.concurrent.RateLimiter;
import com.msas.common.utils.ForeachUtils;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

/**
 * API Controller 통해서 들어온
 * 이메일 전송 예약일 경우에 사용하는 작업 처리기
 */
@Component
@Slf4j
@DisallowConcurrentExecution
public class SendTemplatedEmailJob extends AbstractSendTemplatedEmailJob {

    private final RateLimiter rateLimiter = RateLimiter.create(7.0);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        deleteJobIfNotInterrupted(context);

        log.info("@SendTemplatedEmailJob - started :: jobKey={}", jobKey);

        RequestTemplatedEmailScheduleJobDTO dto = deserializeJobData(context);

        dto.getTemplatedEmailList().forEach(ForeachUtils.withCounter((count, templatedEmail) -> {
            rateLimiter.acquire();

            String messageId = sesMailService.sendTemplatedEmail(getTemplatedEmailDto(dto, count));

            log.info("@SendTemplatedEmailJob - Email sending ({}/{}) : templateName = {}, messageId = {}",
                    count + 1, dto.getTemplatedEmailList().size(), dto.getTemplateName(), messageId);
        }));

        log.info("@SendTemplatedEmailJob - ended :: jobKey={}", jobKey);
    }
}
