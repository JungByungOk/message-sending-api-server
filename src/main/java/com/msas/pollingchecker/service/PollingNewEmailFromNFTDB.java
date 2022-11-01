package com.msas.pollingchecker.service;

import com.amazonaws.services.simpleemail.model.MessageTag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.msas.pollingchecker.model.NewEmailEntity;
import com.msas.pollingchecker.repository.SESMariaDBRepository;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.job.SendTemplatedEmailJob;
import com.msas.scheduler.job.SendTemplatedEmailWithEventJob;
import com.msas.scheduler.service.ScheduleServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollingNewEmailFromNFTDB {

    private final SESMariaDBRepository sesMariaDBRepository;

    private final ScheduleServiceImpl scheduleService;

    @Scheduled(fixedRateString = "${polling.schedule.send-email-check-time:10000}", initialDelay = 15000)
    public void checkNewEmailTask() {
        log.info("⏱️신규 이메일 전송 대기 정보 확인 폴링 <-> MariaDB ");

        List<NewEmailEntity> newEmailEntities = sesMariaDBRepository.findNewEmail();

        if (newEmailEntities.isEmpty())
            return;

        // 발송 요청 건수 합계
        //----------------
        AtomicInteger nSendingEmails = new AtomicInteger();
        newEmailEntities.forEach(newEmailEntity -> {
            nSendingEmails.addAndGet(newEmailEntity.getNewEmailDetailEntities().size());
        });
        log.info("⚠️신규 이메일 발송 요청 [ {} 개 ] 확인", nSendingEmails);

        // 이메일 발송 스케쥴러에 등록
        //------------------------------------------
        newEmailEntities.forEach(newEmailEntity -> {

            RequestTemplatedEmailScheduleJobDTO dto = convertNewEmailEntity2RequestTemplatedEmailScheduleJobDTO(newEmailEntity);

            try {
                scheduleService.addJob(dto, SendTemplatedEmailWithEventJob.class);
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }

            // TODO. 이메일 대기 상태 변경 이벤트 발생 -> 이벤트 수신기에서 상태 변경 처리
            {

            }

        });

    }

    /*
     * 데이타베이스 이메일 등록 정보 -> 쿼츠에 전달할 DTO 변환
     */
    private RequestTemplatedEmailScheduleJobDTO convertNewEmailEntity2RequestTemplatedEmailScheduleJobDTO(NewEmailEntity newEmailEntity) {
        String jobName = String.valueOf(newEmailEntity.getEmail_send_seq());

        String jobGroup = "Default";

        String description = newEmailEntity.getEmail_cls_cd();

        // List<Tag>
        List<MessageTag> tags = new ArrayList<>();
        MessageTag messageTag = new MessageTag();
        {
            messageTag.setName("customTag");
            messageTag.setValue(String.valueOf(newEmailEntity.getEmail_send_seq()));
        }
        tags.add(messageTag);

        RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailDto = new RequestTemplatedEmailScheduleJobDTO();

        // for
        newEmailEntity.getNewEmailDetailEntities().forEach(newEmailDetailEntity -> {

            // TemplateData
            Map<String, String> templateData = new Gson().fromJson(newEmailDetailEntity.getEmail_cts(), HashMap.class);

            // TemplatedEmailList
            List<RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto> TemplatedEmailList = new ArrayList<>();
            RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto templatedEmailDto = new RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto();
            {
                templatedEmailDto.setTo(Collections.singletonList(newEmailDetailEntity.getRcv_email_addr()));
                templatedEmailDto.setTemplateData(templateData);
            }
            TemplatedEmailList.add(templatedEmailDto);

            // 쿼츠 전달용 DTO 설정
            requestTemplatedEmailDto.setJobName(jobName);
            requestTemplatedEmailDto.setJobGroup(jobGroup);
            requestTemplatedEmailDto.setDescription(description);
            requestTemplatedEmailDto.setTemplateName(newEmailDetailEntity.getEmail_tmplet_id());
            requestTemplatedEmailDto.setFrom(newEmailDetailEntity.getSend_email_addr());
            requestTemplatedEmailDto.setTemplatedEmailList(TemplatedEmailList);
            requestTemplatedEmailDto.setTags(tags);
        });

        return requestTemplatedEmailDto;
    }

}
