package com.msas.ses.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(EMailDefaultProperties.PREFIX)
@Getter
@Setter
public class EMailDefaultProperties {

    public static final String PREFIX = "cloud.aws.ses";

    private String sender;
}
