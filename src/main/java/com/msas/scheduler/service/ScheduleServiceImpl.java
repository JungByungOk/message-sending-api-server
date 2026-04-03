package com.msas.scheduler.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.msas.common.utils.DateTimeUtils;
import com.msas.common.utils.LocalDateTimeDeserializer;
import com.msas.common.utils.LocalDateTimeSerializer;
import com.msas.scheduler.dto.JobInfoDTO;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.dto.ResponseAllJobStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    @NotNull
    private final SchedulerFactoryBean schedulerFactoryBean;

    @Override
    public ResponseAllJobStatusDTO getAllJobs() {
        JobInfoDTO jobResponse;
        ResponseAllJobStatusDTO jobStatusResponse = new ResponseAllJobStatusDTO();
        List<JobInfoDTO> jobs = new ArrayList<>();
        int numOfRunningJobs = 0;
        int numOfGroups = 0;
        int numOfAllJobs = 0;

        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            for (String groupName : scheduler.getJobGroupNames()) {
                numOfGroups++;
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                    jobResponse = JobInfoDTO.builder()
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
            log.error("@ScheduleService - error while fetching all job info", e);
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
            log.error("@ScheduleService - error occurred while checking job with jobKey : {}", jobKey, e);
        }
        return false;
    }

    @Override
    public boolean isJobExists(JobKey jobKey) throws SchedulerException {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            if (scheduler.checkExists(jobKey)) {
                return true;
            }
        } catch (SchedulerException e) {
            log.error("@ScheduleService - error occurred while checking job exists :: jobKey : {}", jobKey, e);
            throw e;
        }
        return false;
    }

    @Override
    public void addJob(RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO, Class<? extends Job> jobClass) throws SchedulerException {
        log.info("@ScheduleService - Add job with jobKey : {}", new JobKey(requestTemplatedEmailScheduleJobDTO.getJobGroup(), requestTemplatedEmailScheduleJobDTO.getJobName()));

        /*
         * jobDataMap 에는 primitive type 만 사용하도록 권장한다.
         * 객체를 Serialize/Deserialize 하는데 문제가 발생할 가능성이 있다고 한다.
         * JobDataMap 에 객체를 넣을때는 JSON 으로 직렬화/역직렬화하여 사용하도록 한다.
         */
        JobDataMap jobDataMap = new JobDataMap();
        if (requestTemplatedEmailScheduleJobDTO.getTemplatedEmailList() != null) {

            // 커스텀 시리얼라이즈 설정
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
            gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
            Gson gson = gsonBuilder.setPrettyPrinting().create();

            String jsonSerialized = gson.toJson(requestTemplatedEmailScheduleJobDTO);
            jobDataMap.put("TemplatedEmailScheduleJob", jsonSerialized);

        }

        // 작업 정의
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(requestTemplatedEmailScheduleJobDTO.getJobName(), requestTemplatedEmailScheduleJobDTO.getJobGroup())
                .storeDurably(false)
                .usingJobData(jobDataMap)
                .withDescription(requestTemplatedEmailScheduleJobDTO.getDescription())
                .build();

        Trigger trigger = null;
        if (requestTemplatedEmailScheduleJobDTO.getStartDateAt() == null) {
            // 트리거 정의
            // 반복 없이 즉시 실행하는 트리거
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(requestTemplatedEmailScheduleJobDTO.getJobName(), requestTemplatedEmailScheduleJobDTO.getJobGroup())
                    //.startAt(Timestamp.valueOf(requestTemplatedEmailScheduleJobDTO.getStartDateAt()))    // Type Casting: LocalDateTime to Date
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule() // {SimpleScheduleBuilder, CronScheduleBuilder, CalendarIntervalScheduleBuilder}
                            .withRepeatCount(0)
                            .withMisfireHandlingInstructionFireNow()
                    )
                    .build();
        } else {
            // 트리거 정의
            // 반복 없이 지정한 시간에 한번만 실행하는 트리거
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(requestTemplatedEmailScheduleJobDTO.getJobName(), requestTemplatedEmailScheduleJobDTO.getJobGroup())
                    .startAt(Timestamp.valueOf(requestTemplatedEmailScheduleJobDTO.getStartDateAt()))    // Type Casting: LocalDateTime to Date
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule() // {SimpleScheduleBuilder, CronScheduleBuilder, CalendarIntervalScheduleBuilder}
                            .withRepeatCount(0)
                            .withMisfireHandlingInstructionFireNow()
                    )
                    .build();
        }

        try {
            // 스케쥴러에 등록
            // return Date Format -> Fri Oct 14 23:05:32 KST 2022
            Date registeredDate = schedulerFactoryBean.getScheduler().scheduleJob(jobDetail, trigger);

            log.info("@ScheduleService - Added Schedule Job at {} :: jobKey = {}", registeredDate, jobDetail.getKey().getName());

        } catch (SchedulerException e) {
            log.error("@ScheduleService - error occurred while scheduling with jobKey : {}", jobDetail.getKey());
            log.error("@ScheduleService - {}", e.getMessage());
            throw e;
        }

    }

    /**
     * 예약된 작업의 시간을 변경한다.
     */
    @Override
    public boolean changeTrigger(RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO) {
        // TODO. 예약 시간 변경 구현
        return false;
    }

    /**
     * 예약된 작업 일괄 삭제
     */
    @Override
    public List<JobKey> deleteAllJob() throws SchedulerException {


        List<JobKey> jobKeyList = new ArrayList<>();

        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();

            for (String groupName : scheduler.getJobGroupNames()) {
                jobKeyList.addAll(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName)));
            }

            scheduler.deleteJobs(jobKeyList);

        } catch (SchedulerException e) {
            log.error("@ScheduleService - error occurred while all deleting job with jobKeys : {}", jobKeyList);
            log.error("@ScheduleService - {} : {}", jobKeyList, e.getMessage());
            throw e;
        }

        return jobKeyList;
    }

    /**
     * 실행중인 작업 스레드로을 인터럽트를 호출하여 작업을 중지 시킨다.
     * 다음 예약 시간에 실행 된다.
     */
    @Override
    public void stopJob(JobKey jobKey) throws SchedulerException {

        if (!isJobRunning(jobKey))
            return;

        log.info("@ScheduleService - Stop a running job with jobKey : {}", jobKey);
        try {
            schedulerFactoryBean.getScheduler().interrupt(jobKey); // 작업 중지
            //this.deleteJob(jobKey); // 스케쥴에서 작업 삭제
        } catch (UnableToInterruptJobException e) {
            log.error("@ScheduleService - error occurred while stopping job with jobKey : {}", jobKey, e);
            throw e;
        }
    }

    @Override
    public void deleteJob(JobKey jobKey) throws SchedulerException {
        log.info("@ScheduleService - deleting job with jobKey : {}", jobKey);
        try {
            schedulerFactoryBean.getScheduler().deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error("@ScheduleService - error occurred while deleting job with jobKey : {}", jobKey);
            log.error("@ScheduleService - {} : {}", jobKey, e.getMessage());
            throw e;
        }
    }

    @Override
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        log.info("@ScheduleService - pausing job with jobKey : {}", jobKey);
        try {
            schedulerFactoryBean.getScheduler().pauseJob(jobKey);
        } catch (SchedulerException e) {
            log.error("@ScheduleService - error occurred while deleting job with jobKey : {}", jobKey, e);
            throw e;
        }
    }

    @Override
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        log.info("@ScheduleService - resuming job with jobKey : {}", jobKey);
        try {
            schedulerFactoryBean.getScheduler().resumeJob(jobKey);
        } catch (SchedulerException e) {
            log.error("@ScheduleService - error occurred while resuming job with jobKey : {}", jobKey, e);
            throw e;
        }
    }

    @Override
    public String getJobState(JobKey jobKey) throws SchedulerException {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);

            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());

            if (triggers != null && triggers.size() > 0) {
                for (Trigger trigger : triggers) {
                    // public enum TriggerState { NONE, NORMAL, PAUSED, COMPLETE, ERROR, BLOCKED }
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    if (Trigger.TriggerState.NORMAL.equals(triggerState)) {
                        return "SCHEDULED";
                    }
                    return triggerState.name().toUpperCase();
                }
            }
        } catch (SchedulerException e) {
            log.error("@ScheduleService - Error occurred while getting job state with jobKey : {}", jobKey, e);
            throw e;
        }
        return null;
    }
}