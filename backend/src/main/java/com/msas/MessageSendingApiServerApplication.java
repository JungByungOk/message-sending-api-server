package com.msas;

import com.msas.pollingchecker.properties.AwsDynamoDefaultProperties;
import com.msas.pollingchecker.properties.PollingCheckerProperties;
import com.msas.ses.properties.AwsSesDefaultProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

@SpringBootApplication
@Slf4j
@ConfigurationPropertiesScan({
        PollingCheckerProperties.PREFIX,
        AwsSesDefaultProperties.PREFIX,
        AwsDynamoDefaultProperties.PREFIX,
})
public class MessageSendingApiServerApplication {

    @PostConstruct
    public void started() {
        // 어플리케이션 타임존을 UTC 시간으로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // "Asia/Seoul"

        log.info("어플리케이션 타임존\t -> {}", TimeZone.getDefault().getID());
        log.info("어플리케이션 현재 시간\t -> {}",  LocalDateTime.now());
        log.info("어플리케이션 현재 시간\t -> {}",  LocalDateTime.now().atZone(ZoneId.of("Asia/Seoul")));
        log.info("어플리케이션 현재 시간\t -> {} KST",  ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime());
    }

    public static void main(String[] args) {
        SpringApplication.run(MessageSendingApiServerApplication.class, args);
    }

}
