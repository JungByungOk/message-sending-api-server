package com.msas;

import com.msas.ses.properties.SESDefaultProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(SESDefaultProperties.PREFIX)
public class MessageSendingApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageSendingApiServerApplication.class, args);
    }

}
