package com.msas.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.springframework.stereotype.Component;

/**
 * 트리거가 실행될 때 알림을 받으려는 클래스에 의해 구현되는 인터페이스입니다.
 */
@Slf4j
@Component
public class TriggersListener implements TriggerListener {

    @Override
    public String getName() {
        return "globalTrigger";
    }

    /**
     * 트리거가 호출 되었을 때 스케줄러가 호출하고, 관련된 JobDetail 이 실행되려고 한다.
     */
    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        JobKey jobKey = trigger.getJobKey();
        log.info("triggerFired at {} :: jobKey : {}", trigger.getStartTime(), jobKey);
    }

    /**
     * 트리거가 실행 되었을 때 스케줄러가 호출하고,
     * 관련된 JobDetail 이 실행되려고 한다.
     * 구현이 (true 를 반환함으로써) 실행을 거부하면 작업의 실행 메소드가 호출되지 않습니다.
     */
    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    /**
     * 트리거가 잘못 눌렸을 때 스케줄러가 호출합니다.
     * 이 방법은 오발되는 모든 트리거에 영향을 미치므로이 방법에 소요되는 시간을 고려해야 합니다.
     * 한 번에 많은 트리거가 잘못 발사되면이 방법이 많은 문제가 될 수 있습니다.
     */
    @Override
    public void triggerMisfired(Trigger trigger) {
        JobKey jobKey = trigger.getJobKey();
        log.info("triggerMisfired at {} :: jobKey : {}", trigger.getStartTime(), jobKey);
    }

    /**
     * 트리거가 실행되면 스케줄러가 호출되고 관련된 JobDetail 이 실행되고 트리거(xx) 메서드가 호출됩니다.
     */
    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        JobKey jobKey = trigger.getJobKey();
        log.info("triggerComplete at {} :: jobKey : {}", trigger.getStartTime(), jobKey);
    }
}