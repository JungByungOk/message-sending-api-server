package com.msas.pollingchecker.service;

import com.msas.common.utils.ForeachUtils;
import com.msas.pollingchecker.model.SESEventsEntity;
import com.msas.pollingchecker.repository.SESDynamoDBRepository;
import com.msas.pollingchecker.repository.SESMariaDBRepository;
import com.msas.pollingchecker.types.EnumSESEventTypeCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollingEmailFinalStatusFromDynamoDB {

    private final SESDynamoDBRepository sesEventsDynamoDBRepository;
    private final SESMariaDBRepository sesMariaDBRepository;
    @Value("${spring.application.name}")
    private String serverName;

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

        log.info("⚠️신규 SESMessageId [ {} 개 ] 이벤트 조회", messageIds.size());

        // 1) DynamoDB - messageId 별로 마지막 최종 상태 확인
        // 2) RDBMS 최종 상태 업데이트
        // 3) 처리된 messageId 항목 삭제
        //-----------------------------------------------
        messageIds.forEach(
                ForeachUtils.withCounter(
                        (count, messageId)->
                        {
                            count++;
                            log.info("이메일 상태 업데이트 처리 {}/{} : {}", String.format("%02d", count), messageIds.size(), messageId);

                            // messageId 아이템 중에서 최종 상태 가져오기
                            SESEventsEntity finalStatusEntity = eventList.stream().filter(sesEventsEntity -> sesEventsEntity.getSesMessageId().equals(messageId)).findFirst().get();
                            log.info("\t{} 상태 -> ({})", finalStatusEntity.getSesMessageId(), finalStatusEntity.getEventType());

                            // 최종 상태를 rdbms 업데이트
                            if(UpdateFinalResult(finalStatusEntity) == 0)
                            {
                                log.info("\tDynamoDB Event 삭제 안하고 유지");
                                return; // 이후 진행 안함
                            }

                            // 블랙리스트 등록 처리
                            InsertBlacklistEmail(finalStatusEntity, serverName);

                            // 해당 messageId 항목 전체 DynamoDB 삭제
                            List<SESEventsEntity> deleteItemList = eventList.stream().filter(sesEventsEntity -> sesEventsEntity.getSesMessageId().equals(messageId)).collect(Collectors.toList());
                            DeleteMessageIds(deleteItemList);
                        }//lambda
                )//withCounter
        );//foreach

    }

    private int UpdateFinalResult(SESEventsEntity finalStatusEntity)
    {
        EnumSESEventTypeCode enumSESEventTypeCode = EnumSESEventTypeCode.valueOf(finalStatusEntity.getEventType());
        String send_rslt_typ_cd = enumSESEventTypeCode.name().toUpperCase(Locale.ENGLISH);
        String send_sts_cd = enumSESEventTypeCode.getEmailSendStatusCode().name().toUpperCase(Locale.ENGLISH);

        int result = sesMariaDBRepository.UpdateFinalEmailStatus(finalStatusEntity.getSesMessageId(), send_sts_cd, send_rslt_typ_cd, serverName);

        if(result == 1)
            log.info("\tRDBMS 업데이트 완료");
        else
            log.info("\tRDBMS 업데이트 대상이 없음");

        return result;
    }

    /*
     * 블랙리스트 이메일 등록하기
     */
    private void InsertBlacklistEmail(SESEventsEntity eventsEntity, String serverName)
    {

        // SESEventType {BOUNCE, COMPLAINT, REJECT}는 Blacklist 등록된다.
        EnumSESEventTypeCode enumSESEventTypeCode = EnumSESEventTypeCode.valueOf(eventsEntity.getEventType());

        if(enumSESEventTypeCode == EnumSESEventTypeCode.Bounce ||
                enumSESEventTypeCode == EnumSESEventTypeCode.Complaint ||
                enumSESEventTypeCode == EnumSESEventTypeCode.Reject)
        {

            try
            {
                int result = sesMariaDBRepository.InsertBlacklistEmail(
                        eventsEntity.getDestinationEmail(),
                        enumSESEventTypeCode.name().toUpperCase(Locale.ENGLISH),
                        serverName);

                if(result > 0)
                    log.info("📌 Added Blacklist - {} / {}", eventsEntity.getDestinationEmail(), enumSESEventTypeCode.name().toUpperCase(Locale.ENGLISH));
            }
            catch(DuplicateKeyException e)
            {
                // 중복 키 에러 무시
                e.getMessage();
            }

        } //if
    }

    private void DeleteMessageIds(List<SESEventsEntity> deleteItemList)
    {
        deleteItemList.forEach(sesEventsEntity -> {
            int result = sesEventsDynamoDBRepository
                    .deleteItemBySESMessageIdAndSnsPublishTime(sesEventsEntity.getSesMessageId(), sesEventsEntity.getSnsPublishTime());
        });
    }

}
