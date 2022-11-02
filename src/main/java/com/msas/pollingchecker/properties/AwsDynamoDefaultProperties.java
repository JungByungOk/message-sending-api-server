package com.msas.pollingchecker.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(AwsDynamoDefaultProperties.PREFIX)
@Getter
@Setter
public class AwsDynamoDefaultProperties {

    public static final String PREFIX = "aws.dynamo";

    private String region;
    private String accessKey;
    private String secretKey;

//    private Region region;
//    private Credential credential;
//
//    @Getter
//    @Setter
//    @AllArgsConstructor
//    public static class Region{
//        private String code;
//    }
//
//    @Getter
//    @Setter
//    @AllArgsConstructor
//    public static class Credential {
//        private String accessKey;
//        private String secretKey;
//    }

}
