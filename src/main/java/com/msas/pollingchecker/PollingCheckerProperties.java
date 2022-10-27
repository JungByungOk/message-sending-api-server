package com.msas.pollingchecker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(PollingCheckerProperties.PREFIX)
@Getter
@Setter
public class PollingCheckerProperties {

    public static final String PREFIX = "polling.schedule";

    private long SendEmailCheckTime = 60000;

    private long SendEmailEventCheckTime = 60000;

}
