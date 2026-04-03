package com.msas.ses.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/*
 *  AWS SES 설정
 */
@Configuration
public class AWSSESConfiguration {

    @Value("${aws.ses.region}")
    private String region;

    @Value("${aws.ses.access-key}")
    private String awsAccessKey;

    @Value("${aws.ses.secret-key}")
    private String awsSecretKey;

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)))
                .build();
    }
}
