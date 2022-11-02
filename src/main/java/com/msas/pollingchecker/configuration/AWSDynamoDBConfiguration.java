package com.msas.pollingchecker.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    /**
     * Build the AWS ses client
     * @return AmazonSimpleEmailService
     */
    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .build();
    }

}