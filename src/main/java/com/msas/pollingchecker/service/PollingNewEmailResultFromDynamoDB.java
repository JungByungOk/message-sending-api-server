package com.msas.pollingchecker.service;

import com.msas.common.utils.ForeachUtils;
import com.msas.pollingchecker.model.SESEventsEntity;
import com.msas.pollingchecker.repository.SESEventsDynamoDBRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollingNewEmailResultFromDynamoDB {

    private final SESEventsDynamoDBRepository sesEventsDynamoDBRepository;

    @Scheduled(fixedRateString = "${polling.schedule.send-email-event-check-time:10000}", initialDelay = 10000)
    public void checkNewEmailResultTask()
    {
        log.info("⏱️이메일 전송 결과 이벤트 정보 확인 폴링 <-> DynamoDB ");

        Optional<List<SESEventsEntity>> sesEventsEntityList = sesEventsDynamoDBRepository.getItems();

        // Optional -> list<t> 변환
        //------------------------
        if(sesEventsEntityList.isEmpty())
            return;

        List<SESEventsEntity> eventList = sesEventsEntityList.get().stream()
                .sorted(Comparator.comparing(SESEventsEntity::getSnsPublishTime).reversed())
                .collect(Collectors.toList());

        // messageId 목록 추출
        //------------------
        List<String> messageIds = eventList.stream()
                .filter(p->!p.getCustomTag().isEmpty())
                .map(SESEventsEntity::getSesMessageId)
                .distinct()
                .collect(Collectors.toList());

        log.info("⚠️신규 SESMessageId 이벤트 확인 됨 ({} 개)", messageIds.size());

        // 1) DynamoDB - messageId 별로 마지막 최종 상태 확인
        // 2) RDBMS 업데이트
        // 3) DynamoDB messageId 항목 삭제
        //-----------------------------------------------
        messageIds.forEach(ForeachUtils.withCounter((count, id)-> {

            count++;
            log.info("\t이메일 상태 업데이트 처리 {}/{} : {}", String.format("%02d", count), messageIds.size(), id);

            SESEventsEntity lastStatus = eventList.stream().filter(sesEventsEntity -> sesEventsEntity.getSesMessageId().equals(id)).findFirst().get();

            log.info("\t\t{} 상태 -> ({})", lastStatus.getSesMessageId(), lastStatus.getEventType());

            // todo. rdbms 상태 업데이트 처리

                }
        ));



    }

}
