package com.msas.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 스프링부트 시작 완료 후 발생한 이벤트 처리
 * 스프링부트 시작 완료 후 초기화 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContextRefreshedEventHandler implements ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

    //private final ScheduleService scheduleService;

    @Override
    public void onApplicationEvent(@NotNull org.springframework.context.event.ContextRefreshedEvent contextRefreshedEvent) {
        {
            // TODO. 어플맄케이션 시작하면서 즉시 실행할 스케쥴러 등록

        }
    }
}