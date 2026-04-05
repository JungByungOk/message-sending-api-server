package com.msas.scheduler.job;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.msas.common.utils.LocalDateTimeDeserializer;
import com.msas.common.utils.LocalDateTimeSerializer;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.ses.dto.RequestTemplatedEmailDto;
import com.msas.settings.service.ApiGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;

@Slf4j
public abstract class AbstractSendTemplatedEmailJob extends QuartzJobBean implements InterruptableJob {

    protected ApiGatewayClient apiGatewayClient;

    @Autowired
    public void setApiGatewayClient(ApiGatewayClient apiGatewayClient) {
        this.apiGatewayClient = apiGatewayClient;
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
     * API Gateway를 통해 이메일 발송
     */
    protected String sendTemplatedEmail(RequestTemplatedEmailDto dto) {
        try {
            Gson gson = new Gson();
            String jsonBody = gson.toJson(dto);
            var response = apiGatewayClient.post("/send-email", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                var result = gson.fromJson(response.body(),
                        new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>() {}.getType());
                java.util.Map<String, String> resultMap = result;
                return resultMap.getOrDefault("messageId", "");
            } else {
                log.warn("AbstractSendTemplatedEmailJob - 발송 실패. (HTTP {})", response.statusCode());
                throw new RuntimeException("이메일 발송 실패 (HTTP " + response.statusCode() + ")");
            }
        } catch (Exception e) {
            log.error("AbstractSendTemplatedEmailJob - 발송 실패.", e);
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage(), e);
        }
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
