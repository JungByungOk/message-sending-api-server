package com.msas.pollingchecker.service;

import com.amazonaws.services.simpleemail.model.MessageTag;
import com.google.gson.Gson;
import com.msas.pollingchecker.model.NewEmailEntity;
import com.msas.pollingchecker.repository.SESMariaDBRepository;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollingNewEmailFromNFTDB {

    @Value("${spring.application.name}")
    String serverName;

    private final SESMariaDBRepository sesMariaDBRepository;

    private final ScheduleServiceImpl scheduleService;

    @Scheduled(fixedRateString = "${polling.schedule.send-email-check-time:10000}", initialDelay = 20000)
    public void checkNewEmailTask() {

        log.info("@RDBMS Checking - Are there new email items to send?");

        List<NewEmailEntity> newEmailEntities = sesMariaDBRepository.findNewEmail();

        if (newEmailEntities.isEmpty())
            return;

        // 발송 요청 건수 합계
        //----------------
        AtomicInteger nSendingEmails = new AtomicInteger();
        newEmailEntities.forEach(newEmailEntity -> {
            nSendingEmails.addAndGet(newEmailEntity.getNewEmailDetailEntities().size());
        });
        log.info("@RDBMS Checking - New registered email [ {} ]", nSendingEmails);

        // 이메일 발송 스케쥴러에 등록
        //------------------------------------------
        newEmailEntities.forEach(newEmailEntity -> {

            RequestTemplatedEmailScheduleJobDTO dto = convertNewEmailEntity2RequestTemplatedEmailScheduleJobDTO(newEmailEntity);

            try {
                scheduleService.addJob(dto, SendTemplatedEmailWithPollingJob.class);
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }

            // 이메일 대기 상태 변경 이벤트 발생 -> 이벤트 수신기에서 상태 변경 처리
            //-------------------------------------------------------
            UpdateSendEmailStatus(newEmailEntity);

        });

    }

    /*
     * 처리 결과 업데이트
     */
    private void UpdateSendEmailStatus(NewEmailEntity newEmailEntity) {
        newEmailEntity.getNewEmailDetailEntities().forEach(newEmailDetailEntity -> {

            int result = sesMariaDBRepository.UpdateSendEmailStatus2Scheduler(
                    newEmailDetailEntity.getEmail_send_dtl_seq(),
                    EnumEmailSendStatusCode.SQ.name(),
                    serverName
            );
            log.info("@RDBMS Checking - Email status update with [ SR->SQ ].");

        });
    }

    /*
     * 데이타베이스 이메일 등록 정보 -> 쿼츠에 전달할 DTO 변환
     */
    private RequestTemplatedEmailScheduleJobDTO convertNewEmailEntity2RequestTemplatedEmailScheduleJobDTO(NewEmailEntity newEmailEntity) {

        String jobName = String.valueOf(newEmailEntity.getEmail_send_seq());

        String jobGroup = "Default";

        String description = newEmailEntity.getEmail_cls_cd();

        LocalDateTime startDateAt = newEmailEntity.getRsv_send_dt(); // UTC , null 이면 즉시 실행, 시간이 있으면 예약
        //LocalDateTime startDateAt = LocalDateTime.now().plusYears(1); // for Test

        String templateName = newEmailEntity.getNewEmailDetailEntities().get(0).getEmail_tmplet_id();

        String from = newEmailEntity.getNewEmailDetailEntities().get(0).getSend_email_addr();

        // List<Tag>
        List<MessageTag> tags = new ArrayList<>();
        MessageTag messageTag = new MessageTag();
        {
            messageTag.setName("customTag");
            messageTag.setValue(String.valueOf(newEmailEntity.getEmail_send_seq()));
        }
        tags.add(messageTag);

        // DTO
        RequestTemplatedEmailScheduleJobDTO templatedEmailScheduleDto = new RequestTemplatedEmailScheduleJobDTO();

        // for
        // 개별 이메일 처리
        List<RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto> templatedEmailList = new ArrayList<>();
        newEmailEntity.getNewEmailDetailEntities().forEach(newEmailDetailEntity -> {

            // TemplateData - 탬플릿 변수에 맵핑위한 데이터
            Map<String, String> templateParameter =
                    new Gson().fromJson(newEmailDetailEntity.getEmail_cts(), HashMap.class);

            // TemplatedEmailList - 수신자 + 수신자별 탬플릿 데이터 리스트 (대량 발송 N개)
            RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto templatedEmailDto = new RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto();
            {
                templatedEmailDto.setId(String.valueOf(newEmailDetailEntity.getEmail_send_dtl_seq()));
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
