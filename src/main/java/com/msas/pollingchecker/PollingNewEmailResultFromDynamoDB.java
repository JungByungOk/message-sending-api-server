package com.msas.pollingchecker;

import com.msas.ses.model.SESEventsEntity;
import com.msas.ses.repository.SESEventsDynamoDBRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollingNewEmailResultFromDynamoDB {

    private final SESEventsDynamoDBRepository sesEventsDynamoDBRepository;

    @Scheduled(fixedRateString = "${polling.schedule.send-email-event-check-time:10000}", initialDelay = 60000)
    public void checkNewEmailResulltTask()
    {
        log.info("⏱️이메일 전송 결과 이벤트 정보 확인 폴링 <-> DynamoDB ");

        List<SESEventsEntity> sesEventsEntityList = sesEventsDynamoDBRepository.getItems();

        log.info("⚠️신규 이벤트 확인 됨 = {}", sesEventsEntityList.size());
    }

}
