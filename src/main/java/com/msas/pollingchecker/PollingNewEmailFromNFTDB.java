package com.msas.pollingchecker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PollingNewEmailFromNFTDB {

    @Scheduled(fixedRateString = "${polling.schedule.send-email-check-time:10000}", initialDelay = 60000)
    public void checkNewEmailTask()
    {
        log.info("⏱️신규 이메일 전송 대기 정보 확인 폴링 <-> MariaDB ");



    }

}
