package com.msas.scheduler.service.impl;

import com.msas.scheduler.dto.RequestJob;
import com.msas.scheduler.dto.ResponseJob;
import com.msas.scheduler.dto.ResponseJobStatus;
import com.msas.scheduler.service.ScheduleService;
import com.msas.scheduler.utils.DateTimeUtils;
import com.msas.scheduler.utils.JobUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    @NotNull
    private final SchedulerFactoryBean schedulerFactoryBean;

    @NotNull
    private final ApplicationContext context;

    @Override
    public ResponseJobStatus getAllJobs() {
        ResponseJob jobResponse;
        ResponseJobStatus jobStatusResponse = new ResponseJobStatus();
        List<ResponseJob> jobs = new ArrayList<>();
        int numOfRunningJobs = 0;
        int numOfGroups = 0;
        int numOfAllJobs = 0;

        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            for (String groupName : scheduler.getJobGroupNames()) {
                numOfGroups++;
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                    jobResponse = ResponseJob.builder()
                            .jobName(jobKey.getName())
                            .groupName(jobKey.getGroup())
                            .scheduleTime(DateTimeUtils.toString(triggers.get(0).getStartTime()))
                            .lastFiredTime(DateTimeUtils.toString(triggers.get(0).getPreviousFireTime()))
                            .nextFireTime(DateTimeUtils.toString(triggers.get(0).getNextFireTime()))
                            .build();

                    if (isJobRunning(jobKey)) {
                        jobResponse.setJobStatus("RUNNING");
                        numOfRunningJobs++;
                    } else {
                        String jobState = getJobState(jobKey);
                        jobResponse.setJobStatus(jobState);
                    }
                    numOfAllJobs++;
                    jobs.add(jobResponse);
                }
            }
        } catch (SchedulerException e) {
            log.error("[scheduler-debug] error while fetching all job info", e);
        }

        jobStatusResponse.setNumOfAllJobs(numOfAllJobs);
        jobStatusResponse.setNumOfRunningJobs(numOfRunningJobs);
        jobStatusResponse.setNumOfGroups(numOfGroups);
        jobStatusResponse.setJobs(jobs);
        return jobStatusResponse;
    }

    @Override
    public boolean isJobRunning(JobKey jobKey) {
        try {
            List<JobExecutionContext> currentJobs = schedulerFactoryBean.getScheduler().getCurrentlyExecutingJobs();
            if (currentJobs != null) {
                for (JobExecutionContext jobCtx : currentJobs) {
                    if (jobKey.getName().equals(jobCtx.getJobDetail().getKey().getName())) {
                        return true;
                    }
                }
            }
        } catch (SchedulerException e) {
            log.error("[scheduler-debug] error occurred while checking job with jobKey : {}", jobKey, e);
        }
        return false;
    }

    @Override
    public boolean isJobExists(JobKey jobKey) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            if (scheduler.checkExists(jobKey)) {
                return true;
            }
        } catch (SchedulerException e) {
            log.error("[scheduler-debug] error occurred while checking job exists :: jobKey : {}", jobKey, e);
        }
        return false;
    }

    @Override
    public boolean addJob(RequestJob requestJob, Class<? extends Job> jobClass) {
        JobKey jobKey = null;
        JobDetail jobDetail;
        Trigger trigger;

        try {
            jobKey = JobKey.jobKey(requestJob.getJobName(), requestJob.getJobGroup());
            jobDetail = JobUtils.createJob(requestJob, jobClass, context);
            trigger = JobUtils.createTrigger(requestJob);

            Date dt = schedulerFactoryBean.getScheduler().scheduleJob(jobDetail, trigger);
            log.debug("Job with jobKey : {} scheduled successfully at date : {}", jobDetail.getKey(), dt);
            return true;
        } catch (SchedulerException e) {
            log.error("error occurred while scheduling with jobKey : {}", jobKey, e);
        }
        return false;
    }

    @Override
    public boolean changeTrigger(RequestJob requestJob) {
        return false;
    }

    @Override
    public boolean deleteJob(JobKey jobKey) {
        log.debug("[scheduler-debug] deleting job with jobKey : {}", jobKey);
        try {
            return schedulerFactoryBean.getScheduler().deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error("[scheduler-debug] error occurred while deleting job with jobKey : {}", jobKey, e);
        }
        return false;
    }

    @Override
    public boolean pauseJob(JobKey jobKey) {
        log.debug("[scheduler-debug] pausing job with jobKey : {}", jobKey);
        try {
            schedulerFactoryBean.getScheduler().pauseJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            log.error("[scheduler-debug] error occurred while deleting job with jobKey : {}", jobKey, e);
        }
        return false;
    }

    @Override
    public boolean resumeJob(JobKey jobKey) {
        log.debug("[scheduler-debug] resuming job with jobKey : {}", jobKey);
        try {
            schedulerFactoryBean.getScheduler().resumeJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            log.error("[scheduler-debug] error occurred while resuming job with jobKey : {}", jobKey, e);
        }
        return false;
    }

    @Override
    public String getJobState(JobKey jobKey) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);

            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());

            if (triggers != null && triggers.size() > 0) {
                for (Trigger trigger : triggers) {
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    if (Trigger.TriggerState.NORMAL.equals(triggerState)) {
                        return "SCHEDULED";
                    }
                    return triggerState.name().toUpperCase();
                }
            }
        } catch (SchedulerException e) {
            log.error("[scheduler-debug] Error occurred while getting job state with jobKey : {}", jobKey, e);
        }
        return null;
    }
}