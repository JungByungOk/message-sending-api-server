package com.msas.common.listener;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.msas.scheduler.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 스프링부트 시작 완료 후 발생한 이벤트 처리
 * 스프링부트 시작 완료 후 초기화 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextRefreshedEventHandler implements ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

    private final AmazonDynamoDB amazonDynamoDB;

    private final ScheduleService scheduleService;

    @Override
    public void onApplicationEvent(@NotNull org.springframework.context.event.ContextRefreshedEvent contextRefreshedEvent) {
        {
            // TODO 어플맄케이션 시작하면서 즉시 실행할 스케쥴러 등록

//            Map<String, AttributeValue> key = new HashMap<>();
//            key.put("SESMessageId", (new AttributeValue()).withS("010c0183fb4a3b40-3b8b664c-60ac-44e4-b474-4d1bb39f4459-000000"));
//
//            GetItemRequest getItemRequest = (new GetItemRequest())
//                    .withTableName("SESEvents")
//                    .withKey(key);
//
//            GetItemResult getItemResult = amazonDynamoDB.getItem(getItemRequest);
//
//            log.info("");

        }
    }
}