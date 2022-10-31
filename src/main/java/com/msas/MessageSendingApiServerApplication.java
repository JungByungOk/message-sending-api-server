package com.msas;

import com.msas.pollingchecker.properties.PollingCheckerProperties;
import com.msas.ses.properties.SESDefaultProperties;
import com.msas.telegram.properties.TelegramBotProperties;
import org.joda.time.DateTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan({
        SESDefaultProperties.PREFIX,
        TelegramBotProperties.PREFIX,
        PollingCheckerProperties.PREFIX
})
public class MessageSendingApiServerApplication {

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // "Asia/Seoul"

        System.out.print("\n");
        System.out.printf("  어프리케이션 타임존\t => %s\n", TimeZone.getDefault().getID());
        System.out.printf("  어플리케이션 현재 시간\t => %s\n",  LocalDateTime.now());
        System.out.printf("  어플리케이션 현재 시간\t => %s\n",  LocalDateTime.now().atZone(ZoneId.of("Asia/Seoul")));
        System.out.printf("  어플리케이션 현재 시간\t => %s KST\n",  ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime());
        System.out.print("\n");
    }

    public static void main(String[] args) {
        SpringApplication.run(MessageSendingApiServerApplication.class, args);
    }

}
