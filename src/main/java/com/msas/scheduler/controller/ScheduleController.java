package com.msas.scheduler.controller;

import com.msas.scheduler.dto.RequestScheduleDTO;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.dto.ResponseAllJobStatusDTO;
import com.msas.scheduler.dto.ResponseScheduleDTO;
import com.msas.scheduler.job.SendTemplatedEmailJob;
import com.msas.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

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
    public ResponseEntity<?> addScheduleJob(@Valid @RequestBody RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO) throws SchedulerException {

        // 이미 등록된 작업인지 확인
        if (scheduleService.isJobExists(new JobKey(requestTemplatedEmailScheduleJobDTO.getJobName(), requestTemplatedEmailScheduleJobDTO.getJobGroup()))) {
            return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job already exits"),
                    HttpStatus.BAD_REQUEST);
        }

        // 스케쥴 등록
        scheduleService.addJob(requestTemplatedEmailScheduleJobDTO, SendTemplatedEmailJob.class);

        return new ResponseEntity<>(new ResponseScheduleDTO(true, "Job created successfully"), HttpStatus.CREATED);

    }

    /**
     * ------------------------------------------
     * 예약 작업 삭제
     * ------------------------------------------
     */
    @DeleteMapping("/job")
    public ResponseEntity<?> deleteScheduleJob(@RequestBody RequestScheduleDTO requestScheduleDTO) throws SchedulerException {

        JobKey jobKey = new JobKey(requestScheduleDTO.getJobName(), requestScheduleDTO.getJobGroup());

        if (scheduleService.isJobExists(jobKey)) {
            if (!scheduleService.isJobRunning(jobKey)) {
                scheduleService.deleteJob(jobKey);
            } else {
                return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job already in running state"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job does not exits"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseScheduleDTO(true, "Job deleted successfully"), HttpStatus.OK);

    }

    /**
     * ------------------------------------------
     * 예약된 작업 일괄 삭제
     * ------------------------------------------
     */
    @DeleteMapping("/job/all")
    ResponseEntity<?> deleteScheduleJob() throws SchedulerException {
        List<JobKey> jobKeyList = scheduleService.deleteAllJob();
        return new ResponseEntity<>(new ResponseScheduleDTO(true,
                "All job deleted successfully - " + jobKeyList.toString()), HttpStatus.OK);
    }

    /**
     * ------------------------------------------
     * 예약된 작업 목록
     * ------------------------------------------
     */
    @GetMapping("/jobs")
    public ResponseAllJobStatusDTO getAllJobs() {
        return scheduleService.getAllJobs();
    }

    /**
     * ------------------------------------------
     * 잔행중인 작업 일시 중지
     * ------------------------------------------
     */
    @PutMapping("/job/pause")
    public ResponseEntity<?> pauseJob(@RequestBody RequestScheduleDTO requestScheduleDTO) throws SchedulerException {

        JobKey jobKey = new JobKey(requestScheduleDTO.getJobName(), requestScheduleDTO.getJobGroup());

        if (scheduleService.isJobExists(jobKey)) {
            if (!scheduleService.isJobRunning(jobKey)) {
                scheduleService.pauseJob(jobKey);
            } else {
                return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job already in running state"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job does not exits"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseScheduleDTO(true, "Job paused successfully"), HttpStatus.OK);

    }

    /**
     * ------------------------------------------
     * 일시 중지 작업 재시작
     * ------------------------------------------
     */
    @PutMapping("/job/resume")
    public ResponseEntity<?> resumeJob(@RequestBody RequestScheduleDTO requestScheduleDTO) throws SchedulerException {
        JobKey jobKey = new JobKey(requestScheduleDTO.getJobName(), requestScheduleDTO.getJobGroup());
        if (scheduleService.isJobExists(jobKey)) {
            String jobState = scheduleService.getJobState(jobKey);

            if (jobState.equals("PAUSED")) {
                scheduleService.resumeJob(jobKey);
            } else {
                return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job is not in paused state"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job does not exits"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseScheduleDTO(true, "Job resumed successfully"), HttpStatus.OK);
    }


    /**
     * ------------------------------------------
     * 실행중인 작업 중지
     * ------------------------------------------
     */
    @PutMapping("/job/stop")
    public ResponseEntity<?> stopJob(@RequestBody RequestScheduleDTO requestScheduleDTO) throws SchedulerException {
        JobKey jobKey = new JobKey(requestScheduleDTO.getJobName(), requestScheduleDTO.getJobGroup());

        if (!scheduleService.isJobExists(jobKey))
            return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job does not exits"), HttpStatus.BAD_REQUEST);

        if (!scheduleService.isJobRunning(jobKey))
            return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job is not in running state"), HttpStatus.BAD_REQUEST);

        scheduleService.stopJob(jobKey);
        return new ResponseEntity<>(new ResponseScheduleDTO(true, "Job stop successfully"), HttpStatus.OK);
    }
}
