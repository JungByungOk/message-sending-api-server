package com.msas.scheduler.job;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.msas.common.utils.LocalDateTimeDeserializer;
import com.msas.common.utils.LocalDateTimeSerializer;
import com.msas.common.utils.ForeachUtils;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.ses.dto.RequestTemplatedEmailDto;
import com.msas.ses.service.SESMailService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@DisallowConcurrentExecution // 동시 실행 방지
public class SendTemplatedEmailJob extends QuartzJobBean implements InterruptableJob {
    private final int EMAIL_SEND_DELAY_SECONDS = 2;

    /**
     * 중요!
     * AutowiringSpringBeanJobFactory 에서 서비스 주입을 받기 위해서는
     * 반드시 @Autowired 어노테이션으로만 동작한다.
     */
    private SESMailService sesMailService;
    @Autowired
    public void setSESMailService(SESMailService sesMailService) {
        this.sesMailService = sesMailService;
    }

    private volatile boolean isJobInterrupted = false;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        JobKey jobKey = context.getJobDetail().getKey();
        Thread currThread = Thread.currentThread();

        // 반복 스케쥴일 경우에는, 이전에 interrupt()했으므로 스케쥴에서 제거하고 실행하지 않는다.
        if (!isJobInterrupted) {
            try {
                context.getScheduler().deleteJob(jobKey);
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("⚡SendTemplatedEmailJob started :: jobKey={} - threadName={}", jobKey, currThread.getName());

        //-------------------------------------------------------------------------------
        // 이메일 전송 처리
        //-------------------------------------------------------------------------------
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String strJson = (String) jobDataMap.get("TemplatedEmailScheduleJob");


         // 커스텀 역직렬화 처리
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();

        RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO =
                gson.fromJson(strJson, RequestTemplatedEmailScheduleJobDTO.class);

        //---------------------------------------
        // 이메일 목록이 14개 이상이면,
        // 14개씩 끊어서 전송한다.
        //---------------------------------------
        requestTemplatedEmailScheduleJobDTO.getTemplatedEmailList().forEach(ForeachUtils.withCounter((count, templatedEmail) -> {

            // 이메일 발송
            String messageId = sesMailService.sendTemplatedEmail(getTemplatedEmailDto(requestTemplatedEmailScheduleJobDTO, count));

            log.info("\t이메일 전송 ({}/{}) : templateName = {}, messageId = {}",
                    count + 1,
                    requestTemplatedEmailScheduleJobDTO.getTemplatedEmailList().size(),
                    requestTemplatedEmailScheduleJobDTO.getTemplateName(), messageId);

            //14개 전송 속도 쓰로틀링
            if (count % 14 == 0) {
                try {
                    TimeUnit.SECONDS.sleep(EMAIL_SEND_DELAY_SECONDS); // 14개씩 끊어서 1초 딜레이 전송
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }));

        log.info("⚓SendTemplatedEmailJob ended :: jobKey={} - threadName={}", jobKey, currThread.getName());
    }

    RequestTemplatedEmailDto getTemplatedEmailDto(RequestTemplatedEmailScheduleJobDTO scheduleData, int idx) {
        RequestTemplatedEmailDto requestTemplatedEmailDto = new RequestTemplatedEmailDto();
        requestTemplatedEmailDto.setFrom(scheduleData.getFrom());
        requestTemplatedEmailDto.setTo(scheduleData.getTemplatedEmailList().get(idx).getTo());
        requestTemplatedEmailDto.setTemplateName(scheduleData.getTemplateName());
        requestTemplatedEmailDto.setTemplateData(scheduleData.getTemplatedEmailList().get(idx).getTemplateData());
        requestTemplatedEmailDto.setCc(scheduleData.getTemplatedEmailList().get(idx).getCc());
        requestTemplatedEmailDto.setBcc(scheduleData.getTemplatedEmailList().get(idx).getBcc());
        requestTemplatedEmailDto.setTags(scheduleData.getTags());
        return requestTemplatedEmailDto;
    }

    /**
     * 실행중인 작업 스레드 종료 시키기
     */
    @Override
    public void interrupt() throws UnableToInterruptJobException {
        Thread currThread = Thread.currentThread();
        isJobInterrupted = true;
        log.info("interrupting - {}", currThread.getName());
        currThread.interrupt(); //쓰레드가 일시 정지 상태이면 바로 깨워서 실행시킨다
    }
}
