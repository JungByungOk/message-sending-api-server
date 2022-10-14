package com.msas.common.eventhandler;

import com.msas.scheduler.dto.RequestJob;
import com.msas.scheduler.job.CronJob;
import com.msas.scheduler.job.CronJob2;
import com.msas.scheduler.job.SimpleJob;
import com.msas.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 스프링부트 시작 완료 후 발생한 이벤트 처리
 * 스프링부트 시작 완료 후 초기화 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextRefreshedEventHandler implements ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

    private final ScheduleService scheduleService;

    @Override
    public void onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent contextRefreshedEvent) {

        String str = "\n" +
                "=======================================\n" +
                "⚠️ 어플리케이션 시작\n" +
                "    ✔ Job Scheduling APIs\n" +
                "    ✔ AWS SES APIs\n" +
                "    ✔ Telegram Bot Backend APIs\n" +
                "    ✔ Slack Bot Backend APIs\n" +
                "=======================================";
        log.info(str);

        try {
            testJobSchedule();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void testJobSchedule() throws InterruptedException {

        Thread.sleep(3000);
        //simple job 생성
        RequestJob requestJob = new RequestJob();
        {
            requestJob.setJobName("simpleJob");
            requestJob.setStartDateAt(LocalDateTime.now());
            requestJob.setRepeatCount(50);
            requestJob.setRepeatIntervalInSeconds(30);
        }
        scheduleService.addJob(requestJob, SimpleJob.class);

        //cron job 생성
        JobDataMap jobDataMap = new JobDataMap();
        {
            jobDataMap.put("jobId", "123456789");
            requestJob = new RequestJob();
            requestJob.setJobName("cronJob1");
            requestJob.setCronExpression("0 * * ? * *"); //every min
            requestJob.setJobDataMap(jobDataMap);
        }
        scheduleService.addJob(requestJob, CronJob.class);

        requestJob = new RequestJob();
        {
            requestJob.setJobName("cronJob2");
            requestJob.setCronExpression("0 */5 * ? * *"); //every 5 min
        }
        scheduleService.addJob(requestJob, CronJob2.class);
    }
}