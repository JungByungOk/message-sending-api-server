package com.msas.pollingchecker.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/*
 *  AWS DynamoDB 설정
 */
@Configuration
public class AWSDynamoDBConfiguration {

    @Value("${aws.dynamo.region}")
    private String region;

    @Value("${aws.dynamo.access-key}")
    private String awsAccessKey;

    @Value("${aws.dynamo.secret-key}")
    private String awsSecretKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)))
                .build();
    }
}
