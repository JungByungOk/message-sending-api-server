package com.msas.scheduler.service;

import com.msas.scheduler.dto.RequestJob;
import com.msas.scheduler.dto.ResponseJobStatus;
import org.quartz.Job;
import org.quartz.JobKey;

public interface ScheduleService {
    ResponseJobStatus getAllJobs();

    boolean isJobRunning(JobKey jobKey);

    boolean isJobExists(JobKey jobKey);

//    boolean addJob(JobRequest jobRequest, Class<? extends QuartzJobBean> jobClass);

    boolean addJob(RequestJob requestJob, Class<? extends Job> jobClass);

    boolean deleteJob(JobKey jobKey);

    boolean pauseJob(JobKey jobKey);

    boolean resumeJob(JobKey jobKey);

    String getJobState(JobKey jobKey);
}