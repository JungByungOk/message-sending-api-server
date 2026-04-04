package com.msas.ses.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

import java.net.URI;

/*
 * AWS SES v2 설정
 */
@Configuration
@ConditionalOnExpression("!'${aws.ses.access-key:}'.isEmpty()")
public class AWSSESv2Configuration {

    @Value("${aws.ses.region}")
    private String region;

    @Value("${aws.ses.access-key}")
    private String awsAccessKey;

    @Value("${aws.ses.secret-key}")
    private String awsSecretKey;

    @Value("${aws.endpoint:}")
    private String awsEndpoint;

    @Bean
    public SesV2Client sesV2Client() {
        SesV2ClientBuilder builder = SesV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)));

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint));
        }

        return builder.build();
    }
}
