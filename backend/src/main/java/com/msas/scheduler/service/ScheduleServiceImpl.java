package com.msas.scheduler.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.msas.common.utils.DateTimeUtils;
import com.msas.common.utils.LocalDateTimeDeserializer;
import com.msas.common.utils.LocalDateTimeSerializer;
import com.msas.scheduler.dto.JobInfoDTO;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.dto.ResponseAllJobStatusDTO;
import com.msas.scheduler.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    @NotNull
    private final SchedulerFactoryBean schedulerFactoryBean;

    @NotNull
    private final BatchRepository batchRepository;

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
            log.error("[ScheduleService] 전체 작업 조회 실패.", e);
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
            log.error("[ScheduleService] 작업 실행 상태 확인 실패. (jobKey: {})", jobKey, e);
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
            log.error("[ScheduleService] 작업 존재 여부 확인 실패. (jobKey: {})", jobKey, e);
            throw e;
        }
        return false;
    }

    @Override
    @Transactional
    public void addJob(RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO, Class<? extends Job> jobClass) throws SchedulerException {
        log.info("[ScheduleService] 작업 등록 시작. (jobKey: {})",
                new JobKey(requestTemplatedEmailScheduleJobDTO.getJobGroup(), requestTemplatedEmailScheduleJobDTO.getJobName()));

        /*
         * 이메일 목록을 별도 배치 테이블에 저장하고, JobDataMap에는 batchId만 전달.
         * (기존: 전체 JSON 직렬화 → BLOB 저장 → 대용량 시 메모리/DB 문제)
         */
        JobDataMap jobDataMap = new JobDataMap();
        if (requestTemplatedEmailScheduleJobDTO.getTemplatedEmailList() != null) {
            Gson gson = new Gson();
            String batchId = UUID.randomUUID().toString();

            // 배치 메타 저장
            String tagsJson = requestTemplatedEmailScheduleJobDTO.getTags() != null
                    ? gson.toJson(requestTemplatedEmailScheduleJobDTO.getTags()) : null;
            batchRepository.insertBatch(
                    batchId,
                    requestTemplatedEmailScheduleJobDTO.getTemplateName(),
                    requestTemplatedEmailScheduleJobDTO.getFrom(),
                    tagsJson,
                    requestTemplatedEmailScheduleJobDTO.getJobName(),
                    requestTemplatedEmailScheduleJobDTO.getJobGroup(),
                    requestTemplatedEmailScheduleJobDTO.getDescription(),
                    requestTemplatedEmailScheduleJobDTO.getStartDateAt(),
                    requestTemplatedEmailScheduleJobDTO.getTemplatedEmailList().size(),
                    requestTemplatedEmailScheduleJobDTO.getTenantId()
            );

            // 배치 항목 저장 (500건씩 묶어서 배치 INSERT)
            List<RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto> emailList =
                    requestTemplatedEmailScheduleJobDTO.getTemplatedEmailList();
            int batchSize = 500;
            List<Map<String, Object>> batchItems = new ArrayList<>();
            for (int i = 0; i < emailList.size(); i++) {
                RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto item = emailList.get(i);
                Map<String, Object> row = new HashMap<>();
                row.put("batch_id", batchId);
                row.put("item_index", i);
                row.put("correlation_id", item.getCorrelationId());
                row.put("email_send_dtl_seq", item.getId() != null ? Integer.parseInt(item.getId()) : null);
                row.put("to_addrs", gson.toJson(item.getTo()));
                row.put("cc_addrs", item.getCc() != null ? gson.toJson(item.getCc()) : null);
                row.put("bcc_addrs", item.getBcc() != null ? gson.toJson(item.getBcc()) : null);
                row.put("template_parameters", item.getTemplateParameters() != null ? gson.toJson(item.getTemplateParameters()) : null);
                batchItems.add(row);

                if (batchItems.size() >= batchSize || i == emailList.size() - 1) {
                    batchRepository.insertBatchItems(batchItems);
                    batchItems.clear();
                }
            }

            log.info("[ScheduleService] 배치 저장 완료. (batchId: {}, 건수: {})", batchId, emailList.size());
            jobDataMap.put("batchId", batchId);
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

            log.info("[ScheduleService] 작업 등록 완료. (jobKey: {}, registeredAt: {})",
                    jobDetail.getKey().getName(), registeredDate);

        } catch (SchedulerException e) {
            log.error("[ScheduleService] 작업 등록 실패. (jobKey: {}, message: {})",
                    jobDetail.getKey(), e.getMessage());
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
            log.error("[ScheduleService] 전체 작업 삭제 실패. (jobKeys: {}, message: {})",
                    jobKeyList, e.getMessage());
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

        log.info("[ScheduleService] 작업 중지 요청. (jobKey: {})", jobKey);
        try {
            schedulerFactoryBean.getScheduler().interrupt(jobKey); // 작업 중지
            //this.deleteJob(jobKey); // 스케쥴에서 작업 삭제
        } catch (UnableToInterruptJobException e) {
            log.error("[ScheduleService] 작업 중지 실패. (jobKey: {})", jobKey, e);
            throw e;
        }
    }

    @Override
    public void deleteJob(JobKey jobKey) throws SchedulerException {
        log.info("[ScheduleService] 작업 삭제 요청. (jobKey: {})", jobKey);
        try {
            schedulerFactoryBean.getScheduler().deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error("[ScheduleService] 작업 삭제 실패. (jobKey: {}, message: {})",
                    jobKey, e.getMessage());
            throw e;
        }
    }

    @Override
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        log.info("[ScheduleService] 작업 일시 중지 요청. (jobKey: {})", jobKey);
        try {
            schedulerFactoryBean.getScheduler().pauseJob(jobKey);
        } catch (SchedulerException e) {
            log.error("[ScheduleService] 작업 일시 중지 실패. (jobKey: {})", jobKey, e);
            throw e;
        }
    }

    @Override
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        log.info("[ScheduleService] 작업 재개 요청. (jobKey: {})", jobKey);
        try {
            schedulerFactoryBean.getScheduler().resumeJob(jobKey);
        } catch (SchedulerException e) {
            log.error("[ScheduleService] 작업 재개 실패. (jobKey: {})", jobKey, e);
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
            log.error("[ScheduleService] 작업 상태 조회 실패. (jobKey: {})", jobKey, e);
            throw e;
        }
        return null;
    }
}
