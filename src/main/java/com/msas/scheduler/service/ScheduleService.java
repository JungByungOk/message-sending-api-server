package com.msas.scheduler.service;

import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.dto.ResponseAllJobStatusDTO;
import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

public interface ScheduleService {

    /**
     * 전체 스케쥴 작업 조회
     */
    ResponseAllJobStatusDTO getAllJobs();

    /**
     * 작업 상태 조회
     */
    boolean isJobRunning(JobKey jobKey);

    /**
     * 스케쥴 이름 중복 검사
     */
    boolean isJobExists(JobKey jobKey) throws SchedulerException;

    //boolean addJob(JobRequest jobRequest, Class<? extends QuartzJobBean> jobClass);

    /**
     * 신규 작업 스케쥴 등록
     */
    void addJob(RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO, Class<? extends Job> jobClass) throws SchedulerException;

    /**
     * 스케쥴 작업 삭제
     */
    void deleteJob(JobKey jobKey) throws SchedulerException;

    /**
     * 스케쥴 작업 일시 멈춤
     */
    void pauseJob(JobKey jobKey) throws SchedulerException;

    /**
     * 스케쥴 작업 재시작
     */
    void resumeJob(JobKey jobKey) throws SchedulerException;

    /**
     * 스케쥴 작업 상태
     */
    String getJobState(JobKey jobKey) throws SchedulerException;

    void stopJob(JobKey jobKey) throws SchedulerException;

    /**
     * 기존 등록된 스케쥴 작업의 예약 시간을 변경 (OldTrigger -> NewTrigger)
     */
    boolean changeTrigger(RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO);

    /**
     * 스케쥴 전체 일괄 삭제
     */
    void deleteAllJob(JobKey jobKey) throws SchedulerException;
}