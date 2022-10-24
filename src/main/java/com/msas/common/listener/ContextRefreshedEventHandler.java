package com.msas.common.listener;

import com.msas.scheduler.service.ScheduleService;
import com.msas.ses.repository.SESEventsDynamoDBRepository;
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

    private final SESEventsDynamoDBRepository sesEventsDynamoDBRepository;

    private final ScheduleService scheduleService;

    @Override
    public void onApplicationEvent(@NotNull org.springframework.context.event.ContextRefreshedEvent contextRefreshedEvent) {
        {
            // TODO 어플맄케이션 시작하면서 즉시 실행할 스케쥴러 등록

//            Map<String, AttributeValue> key = new HashMap<>();
//            key.put("CustomTag", (new AttributeValue()).withS("20221024213100"));
//
//            GetItemRequest getItemRequest = (new GetItemRequest())
//                    .withTableName("SESEvents")
//                    .withKey(key);
//
//            GetItemResult getItemResult = amazonDynamoDB.getItem(getItemRequest);

            ///////////////////////////////////////////////////////////////////////////

//            DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
//            Table table = dynamoDB.getTable("SESEvents");
//            Item item = table
//                    .getItem("SESMessageId", "010c01840a718887-9d7e5844-c491-4a0d-bec9-1d18099a7a8e-000000",
//                            "SnsPublishTime", "2022-10-24T14:42:06.110Z");
//
//            String sqlStatement = "select * from ? where CustomTag=?";
//
//            List<AttributeValue> parameters = new ArrayList<>();
//            AttributeValue att1 = new AttributeValue().withS("SESEvents").withS("20221024213100");
//
//
//            ExecuteStatementRequest.Builder()
//
//            amazonDynamoDB.executeStatement(String.format(, "SESEvents", "20221024213100"));
//
//            log.info("");

            sesEventsDynamoDBRepository.getItems("20221024213100");

        }
    }
}