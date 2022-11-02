package com.msas.ses.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(AwsSesDefaultProperties.PREFIX)
@Getter
@Setter
public class AwsSesDefaultProperties {

    public static final String PREFIX = "aws.ses";

    private String region;
    private String accessKey;
    private String secretKey;

//    private Region region;
//    private Credential credential;
//
//    @AllArgsConstructor
//    @Getter
//    @Setter
//    public static class Region{
//        private String code;
//    }
//
//    @AllArgsConstructor
//    @Getter
//    @Setter
//    public static class Credential {
//        private String accessKey;
//        private String secretKey;
//    }

}
