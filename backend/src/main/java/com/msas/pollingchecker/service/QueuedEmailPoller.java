package com.msas.pollingchecker.service;

import com.msas.ses.dto.MessageTagDto;
import com.google.gson.Gson;
import com.msas.pollingchecker.model.NewEmailEntity;
import com.msas.pollingchecker.repository.EmailSendRepository;
import com.msas.pollingchecker.types.EnumEmailSendStatusCode;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.job.SendTemplatedEmailWithPollingJob;
import com.msas.scheduler.service.ScheduleServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueuedEmailPoller {

    @Value("${spring.application.name}")
    String serverName;

    private final EmailSendRepository emailSendRepository;

    private final ScheduleServiceImpl scheduleService;

    @Transactional
    @Scheduled(fixedRateString = "${polling.schedule.send-email-check-time:10000}", initialDelay = 20000)
    public void checkNewEmailTask() {

        log.info("[PollingChecker] 신규 발송 이메일 확인 중...");

        List<NewEmailEntity> newEmailEntities = emailSendRepository.findNewEmail();

        if (newEmailEntities.isEmpty())
            return;

        // 발송 요청 건수 합계
        //----------------
        AtomicInteger nSendingEmails = new AtomicInteger();
        newEmailEntities.forEach(newEmailEntity -> {
            nSendingEmails.addAndGet(newEmailEntity.getNewEmailDetailEntities().size());
        });
        log.info("[PollingChecker] 신규 이메일 발견. (건수: {})", nSendingEmails);

        // 이메일 발송 스케쥴러에 등록
        //------------------------------------------
        newEmailEntities.forEach(newEmailEntity -> {

            if (newEmailEntity.getNewEmailDetailEntities() == null || newEmailEntity.getNewEmailDetailEntities().isEmpty()) {
                log.warn("[PollingChecker] 상세 목록이 비어 있어 건너뜀. (email_send_seq: {})", newEmailEntity.getEmail_send_seq());
                return;
            }

            // 먼저 상태를 SQ로 변경하여 다음 폴링에서 재조회 방지
            UpdateSendEmailStatus(newEmailEntity);

            RequestTemplatedEmailScheduleJobDTO dto = convertNewEmailEntity2RequestTemplatedEmailScheduleJobDTO(newEmailEntity);

            try {
                scheduleService.addJob(dto, SendTemplatedEmailWithPollingJob.class);
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }

        });

    }

    /*
     * 처리 결과 업데이트
     */
    private void UpdateSendEmailStatus(NewEmailEntity newEmailEntity) {
        newEmailEntity.getNewEmailDetailEntities().forEach(newEmailDetailEntity -> {

            int result = emailSendRepository.UpdateSendEmailStatus2Scheduler(
                    newEmailDetailEntity.getEmail_send_dtl_seq(),
                    EnumEmailSendStatusCode.Queued.name(),
                    serverName
            );
            log.info("[PollingChecker] 이메일 상태 변경. (-> Queued, dtlSeq: {})",
                    newEmailDetailEntity.getEmail_send_dtl_seq());

        });
    }

    /*
     * 데이타베이스 이메일 등록 정보 -> 쿼츠에 전달할 DTO 변환
     */
    private RequestTemplatedEmailScheduleJobDTO convertNewEmailEntity2RequestTemplatedEmailScheduleJobDTO(NewEmailEntity newEmailEntity) {

        String jobName = String.valueOf(newEmailEntity.getEmail_send_seq());

        String jobGroup = "DEFAULT";

        String description = newEmailEntity.getEmail_cls_cd();

        LocalDateTime startDateAt = newEmailEntity.getRsv_send_dt(); // UTC , null 이면 즉시 실행, 시간이 있으면 예약
        //LocalDateTime startDateAt = LocalDateTime.now().plusYears(1); // for Test

        String templateName = newEmailEntity.getNewEmailDetailEntities().get(0).getEmail_tmplet_id();

        String from = newEmailEntity.getNewEmailDetailEntities().get(0).getSend_email_addr();

        // List<Tag>
        List<MessageTagDto> tags = new ArrayList<>();
        tags.add(new MessageTagDto("customTag", String.valueOf(newEmailEntity.getEmail_send_seq())));

        // DTO
        RequestTemplatedEmailScheduleJobDTO templatedEmailScheduleDto = new RequestTemplatedEmailScheduleJobDTO();

        // for
        // 개별 이메일 처리
        List<RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto> templatedEmailList = new ArrayList<>();
        newEmailEntity.getNewEmailDetailEntities().forEach(newEmailDetailEntity -> {

            // TemplateData - 탬플릿 변수에 맵핑위한 데이터
            Map<String, String> templateParameter = new HashMap<>();
            String emailCts = newEmailDetailEntity.getEmail_cts();
            if (emailCts != null && emailCts.trim().startsWith("{")) {
                try {
                    templateParameter = new Gson().fromJson(emailCts, HashMap.class);
                } catch (Exception e) {
                    log.warn("[PollingChecker] 템플릿 파라미터 파싱 실패. 텍스트 이메일로 간주. (dtlSeq: {})",
                            newEmailDetailEntity.getEmail_send_dtl_seq());
                }
            }

            // TemplatedEmailList - 수신자 + 수신자별 탬플릿 데이터 리스트 (대량 발송 N개)
            RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto templatedEmailDto = new RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto();
            {
                // correlationId가 없으면 생성하여 DB에 저장
                String correlationId = newEmailDetailEntity.getCorrelation_id();
                if (correlationId == null || correlationId.isEmpty()) {
                    correlationId = java.util.UUID.randomUUID().toString();
                    emailSendRepository.UpdateSendEmailStatus2AWSSES(
                            newEmailDetailEntity.getEmail_send_dtl_seq(),
                            "Queued", null, null, serverName);
                }

                templatedEmailDto.setId(String.valueOf(newEmailDetailEntity.getEmail_send_dtl_seq()));
                templatedEmailDto.setCorrelationId(correlationId);
                templatedEmailDto.setTo(Collections.singletonList(newEmailDetailEntity.getRcv_email_addr()));
                templatedEmailDto.setTemplateParameters(templateParameter);
            }
            templatedEmailList.add(templatedEmailDto);

        });

        // 쿼츠 전달용 대량 발송 DTO 설정
        templatedEmailScheduleDto.setJobName(jobName);
        templatedEmailScheduleDto.setJobGroup(jobGroup);
        templatedEmailScheduleDto.setDescription(description);
        templatedEmailScheduleDto.setStartDateAt(startDateAt);
        templatedEmailScheduleDto.setTemplateName(templateName);
        templatedEmailScheduleDto.setFrom(from);
        {
            templatedEmailScheduleDto.setTemplatedEmailList(templatedEmailList);
        }
        templatedEmailScheduleDto.setTags(tags);

        return templatedEmailScheduleDto;
    }

}
