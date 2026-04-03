package com.msas.scheduler.listener;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.springframework.stereotype.Component;

/**
 * 예약된 JobDetail 이 실행될 때 알림을 받고자 하는 인터페이스 구현
 */
@Slf4j
@Component
public class JobsListener implements JobListener {

    @Override
    public String getName() {
        return "globalJob";
    }

    /**
     * JobDetail 이 실행되려고 할 때 (관련 트리거가 발생했을 때) 스케줄러에 의해 호출됩니다.
     * 이 메서드는 작업 실행이 TriggerListener 에 의해 거부된 경우 호출되지 않습니다.
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        JobKey jobKey = context.getJobDetail().getKey();
        log.info("jobToBeExecuted :: jobKey = {}", jobKey);
    }

    /**
     * JobDetail 이 실행되려고 할 때 스케줄러에 의해 호출되었지만(관련 트리거가 발생했음),
     * TriggerListener 는 실행에 거부권을 행사했다.
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        JobKey jobKey = context.getJobDetail().getKey();
        log.info("jobExecutionVetoed :: jobKey = {}", jobKey);
    }

    /**
     * JobDetail 이 실행된 후 스케줄러에 의해 호출되고 관련 Trigger 의 trigger(xx) 메서드가 호출되었습니다.
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        JobKey jobKey = context.getJobDetail().getKey();
        log.info("jobWasExecuted :: jobKey = {}", jobKey);
    }
}