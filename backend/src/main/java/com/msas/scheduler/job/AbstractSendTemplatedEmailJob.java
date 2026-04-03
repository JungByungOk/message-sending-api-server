package com.msas.scheduler.job;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.msas.common.utils.LocalDateTimeDeserializer;
import com.msas.common.utils.LocalDateTimeSerializer;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.ses.dto.RequestTemplatedEmailDto;
import com.msas.ses.service.SESMailService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;

@Slf4j
public abstract class AbstractSendTemplatedEmailJob extends QuartzJobBean implements InterruptableJob {

    protected SESMailService sesMailService;

    @Autowired
    public void setSESMailService(SESMailService sesMailService) {
        this.sesMailService = sesMailService;
    }

    protected volatile boolean isJobInterrupted = false;

    /**
     * JobDataMap에서 DTO 역직렬화
     */
    protected RequestTemplatedEmailScheduleJobDTO deserializeJobData(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String strJson = (String) jobDataMap.get("TemplatedEmailScheduleJob");

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();

        return gson.fromJson(strJson, RequestTemplatedEmailScheduleJobDTO.class);
    }

    /**
     * 반복 스케줄 시 이전 interrupt된 Job을 스케줄에서 제거
     */
    protected void deleteJobIfNotInterrupted(JobExecutionContext context) throws JobExecutionException {
        if (!isJobInterrupted) {
            try {
                context.getScheduler().deleteJob(context.getJobDetail().getKey());
            } catch (SchedulerException e) {
                throw new JobExecutionException(e);
            }
        }
    }

    /**
     * 대량 발송 이메일 그룹에서 개별 이메일 정보 가져오기
     */
    protected RequestTemplatedEmailDto getTemplatedEmailDto(RequestTemplatedEmailScheduleJobDTO scheduleData, int idx) {
        RequestTemplatedEmailDto dto = new RequestTemplatedEmailDto();
        dto.setFrom(scheduleData.getFrom());
        dto.setTo(scheduleData.getTemplatedEmailList().get(idx).getTo());
        dto.setTemplateName(scheduleData.getTemplateName());
        dto.setTemplateData(scheduleData.getTemplatedEmailList().get(idx).getTemplateParameters());
        dto.setCc(scheduleData.getTemplatedEmailList().get(idx).getCc());
        dto.setBcc(scheduleData.getTemplatedEmailList().get(idx).getBcc());
        dto.setTags(scheduleData.getTags());
        return dto;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        isJobInterrupted = true;
        log.info("@{} - thread interrupting (stop working)", getClass().getSimpleName());
    }
}
