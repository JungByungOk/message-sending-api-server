package com.msas.scheduler.configuration;

import com.msas.scheduler.dto.RequestJob;
import com.msas.scheduler.job.CronJob;
import com.msas.scheduler.job.CronJob2;
import com.msas.scheduler.job.SimpleJob;
import com.msas.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private final ScheduleService scheduleService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
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