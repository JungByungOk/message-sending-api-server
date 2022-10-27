package com.msas;

import com.msas.pollingchecker.PollingCheckerProperties;
import com.msas.ses.properties.SESDefaultProperties;
import com.msas.telegram.properties.TelegramBotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan({
        SESDefaultProperties.PREFIX,
        TelegramBotProperties.PREFIX,
        PollingCheckerProperties.PREFIX
})
public class MessageSendingApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageSendingApiServerApplication.class, args);
    }

}
