package com.msas.common.listener;

import com.msas.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 스프링부트 시작 완료 후 발생한 이벤트 처리
 * 스프링부트 시작 완료 후 초기화 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextRefreshedEventHandler implements ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

    private final ScheduleService scheduleService;

    @Override
    public void onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent contextRefreshedEvent) {

        String str = "\n" +
                "=======================================\n" +
                "⚠️ 어플리케이션 시작\n" +
                "    ✔ AWS SES APIs\n" +
                "    ✔ Email Scheduling APIs\n" +
                "    ✔ Telegram Bot Backend APIs (다중 채널 선택 전송)\n" +
                "    \n" +
                "    🚫 Slack Bot Backend APIs\n" +
                "    🚫 RAM Store -> Database (Job, Trigger, History)\n" +
                "=======================================";
        log.info(str);

    }
}