package com.msas.scheduler.controller;

import com.msas.scheduler.dto.RequestJob;
import com.msas.scheduler.dto.ResponseJobStatus;
import com.msas.scheduler.dto.ResponseScheduler;
import com.msas.scheduler.job.CronJob;
import com.msas.scheduler.job.SimpleJob;
import com.msas.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * ------------------------------------------
     * 예약 작업 추가 (등록)
     * ------------------------------------------
     */
    @PostMapping("/job")
    public ResponseEntity<?> addScheduleJob(@Valid @RequestBody RequestJob requestJob) {

        log.debug("add schedule job :: requestJob : {}", requestJob);

        if (requestJob.getJobName() == null) {
            return new ResponseEntity<>(
                    new ResponseScheduler(false, "Require jobName"),
                    HttpStatus.BAD_REQUEST);
        }

        JobKey jobKey = new JobKey(requestJob.getJobName(), requestJob.getJobGroup());
        if (!scheduleService.isJobExists(jobKey)) {
            if (requestJob.getCronExpression() == null) {
                scheduleService.addJob(requestJob, SimpleJob.class);
            } else {
                scheduleService.addJob(requestJob, CronJob.class);
            }
        } else {
            return new ResponseEntity<>(new ResponseScheduler(false, "Job already exits"),
                    HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseScheduler(true, "Job created successfully"), HttpStatus.CREATED);

    }

    /**
     * ------------------------------------------
     * 예약 작업 삭제
     * ------------------------------------------
     */
    @DeleteMapping("/job")
    public ResponseEntity<?> deleteScheduleJob(@RequestBody RequestJob jobRequest) {

        JobKey jobKey = new JobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        if (scheduleService.isJobExists(jobKey)) {
            if (!scheduleService.isJobRunning(jobKey)) {
                scheduleService.deleteJob(jobKey);
            } else {
                return new ResponseEntity<>(new ResponseScheduler(false, "Job already in running state"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new ResponseScheduler(false, "Job does not exits"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseScheduler(true, "Job deleted successfully"), HttpStatus.OK);

    }

    /**
     * ------------------------------------------
     * 예약된 작업 목록
     * ------------------------------------------
     */
    @GetMapping("/jobs")
    public ResponseJobStatus getAllJobs() {
        return scheduleService.getAllJobs();
    }

    /**
     * ------------------------------------------
     * 잔행중인 작업 일시 중지
     * ------------------------------------------
     */
    @PutMapping("/job/pause")
    public ResponseEntity<?> pauseJob(@RequestBody RequestJob jobRequest) {

        JobKey jobKey = new JobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        if (scheduleService.isJobExists(jobKey)) {
            if (!scheduleService.isJobRunning(jobKey)) {
                scheduleService.pauseJob(jobKey);
            } else {
                return new ResponseEntity<>(new ResponseScheduler(false, "Job already in running state"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new ResponseScheduler(false, "Job does not exits"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseScheduler(true, "Job paused successfully"), HttpStatus.OK);

    }

    /**
     * ------------------------------------------
     * 일시 중지 작업 재시작
     * ------------------------------------------
     */
    @PutMapping("/job/resume")
    public ResponseEntity<?> resumeJob(@RequestBody RequestJob jobRequest) {
        JobKey jobKey = new JobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        if (scheduleService.isJobExists(jobKey)) {
            String jobState = scheduleService.getJobState(jobKey);

            if (jobState.equals("PAUSED")) {
                scheduleService.resumeJob(jobKey);
            } else {
                return new ResponseEntity<>(new ResponseScheduler(false, "Job is not in paused state"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new ResponseScheduler(false, "Job does not exits"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseScheduler(true, "Job resumed successfully"), HttpStatus.OK);
    }
}
