package com.msas.pollingchecker.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/*
 *  AWS DynamoDB 설정
 */
@Configuration
@ConditionalOnExpression("!'${aws.dynamo.access-key:}'.isEmpty()")
public class AWSDynamoDBConfiguration {

    @Value("${aws.dynamo.region}")
    private String region;

    @Value("${aws.dynamo.access-key}")
    private String awsAccessKey;

    @Value("${aws.dynamo.secret-key}")
    private String awsSecretKey;

    @Value("${aws.endpoint:}")
    private String awsEndpoint;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)));

        if (awsEndpoint != null && !awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint));
        }

        return builder.build();
    }
}
