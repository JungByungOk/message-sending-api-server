package com.msas.scheduler.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.msas.scheduler.dto.RequestScheduleDTO;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.dto.ResponseAllJobStatusDTO;
import com.msas.scheduler.dto.ResponseScheduleDTO;
import com.msas.scheduler.job.SendTemplatedEmailJob;
import com.msas.scheduler.service.ScheduleService;
import com.msas.common.tenant.TenantContext;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Scheduler", description = "Quartz 기반 예약 발송 관리 API")
@Slf4j
@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TenantRepository tenantRepository;

    @Operation(summary = "예약 작업 생성", description = "템플릿 이메일 예약 발송 작업을 등록합니다.")
    @PostMapping("/job")
    public ResponseEntity<?> addScheduleJob(@Valid @RequestBody RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO,
                                               HttpServletRequest request) throws SchedulerException {

        // 이미 등록된 작업인지 확인
        if (scheduleService.isJobExists(new JobKey(requestTemplatedEmailScheduleJobDTO.getJobName(), requestTemplatedEmailScheduleJobDTO.getJobGroup()))) {
            return new ResponseEntity<>(new ResponseScheduleDTO(false, "Job already exits"),
                    HttpStatus.BAD_REQUEST);
        }

        // tenantId 설정: TenantContext 우선, 없으면 Authorization 헤더에서 조회
        requestTemplatedEmailScheduleJobDTO.setTenantId(resolveTenantId(request));

        // 스케쥴 등록
        scheduleService.addJob(requestTemplatedEmailScheduleJobDTO, SendTemplatedEmailJob.class);

        return new ResponseEntity<>(new ResponseScheduleDTO(true, "Job created successfully"), HttpStatus.CREATED);

    }

    @Operation(summary = "작업 삭제", description = "지정한 예약 발송 작업을 삭제합니다. 실행 중인 작업은 삭제할 수 없습니다.")
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

    @Operation(summary = "전체 작업 삭제", description = "등록된 모든 예약 발송 작업을 일괄 삭제합니다.")
    @DeleteMapping("/job/all")
    ResponseEntity<?> deleteScheduleJob() throws SchedulerException {
        List<JobKey> jobKeyList = scheduleService.deleteAllJob();
        return new ResponseEntity<>(new ResponseScheduleDTO(true,
                "All job deleted successfully - " + jobKeyList.toString()), HttpStatus.OK);
    }

    @Operation(summary = "작업 목록 조회", description = "등록된 모든 예약 작업의 상태를 조회합니다.")
    @GetMapping("/jobs")
    public ResponseAllJobStatusDTO getAllJobs() {
        return scheduleService.getAllJobs();
    }

    @Operation(summary = "작업 일시정지", description = "지정한 작업을 일시정지합니다. 실행 중인 작업은 일시정지할 수 없습니다.")
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

    @Operation(summary = "작업 재개", description = "일시정지된 작업을 재개합니다.")
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


    @Operation(summary = "작업 중지", description = "실행 중인 작업을 중지합니다.")
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

    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) return tenantId;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String apiKey = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
            TenantEntity tenant = tenantRepository.selectTenantByApiKey(apiKey);
            if (tenant != null) return tenant.getTenantId();
        }
        return null;
    }
}
