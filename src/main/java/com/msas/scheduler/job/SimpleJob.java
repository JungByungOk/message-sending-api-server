package com.msas.scheduler.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class SimpleJob extends QuartzJobBean {
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        Thread currThread = Thread.currentThread();

        log.info("@SimpleJob - started :: jobKey={} - threadName={}", jobKey, currThread.getName());
        {

        }
        log.info("@SimpleJob - ended :: jobKey={} - threadName={}", jobKey, currThread.getName());
    }
}