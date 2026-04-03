package com.msas.pollingchecker.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msas.pollingchecker.model.SESEventsEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnBean(DynamoDbClient.class)
public class SESDynamoDBRepository {

    private final DynamoDbClient dynamoDbClient;

    public Optional<List<SESEventsEntity>> getItems() {
        List<SESEventsEntity> sesEventsEntityList = null;

        try {
            ExecuteStatementRequest request = ExecuteStatementRequest.builder()
                    .limit(300)
                    .statement("select * from SESEvents;")
                    .build();

            ExecuteStatementResponse response = dynamoDbClient.executeStatement(request);
            sesEventsEntityList = toList(response);

        } catch (Exception e) {
            handleExecuteStatementErrors(e);
        }

        return Optional.ofNullable(sesEventsEntityList);
    }

    public Optional<List<SESEventsEntity>> getItemsByCustomTag(String customTag) {
        List<SESEventsEntity> sesEventsEntityList = null;

        try {
            ExecuteStatementRequest request = ExecuteStatementRequest.builder()
                    .statement("select * from SESEvents where CustomTag=?;")
                    .parameters(AttributeValue.builder().s(customTag).build())
                    .build();

            ExecuteStatementResponse response = dynamoDbClient.executeStatement(request);
            sesEventsEntityList = toList(response);

        } catch (Exception e) {
            handleExecuteStatementErrors(e);
        }

        return Optional.ofNullable(sesEventsEntityList);
    }

    public int deleteItemBySESMessageIdAndSnsPublishTime(String sesMessageId, String snsPublishTime) {
        int result = 0;

        try {
            ExecuteStatementRequest request = ExecuteStatementRequest.builder()
                    .statement("delete from SESEvents where SESMessageId=? and SnsPublishTime=? RETURNING ALL OLD *;")
                    .parameters(
                            AttributeValue.builder().s(sesMessageId).build(),
                            AttributeValue.builder().s(snsPublishTime).build()
                    )
                    .build();

            ExecuteStatementResponse response = dynamoDbClient.executeStatement(request);
            result = response.items().size();

        } catch (Exception e) {
            handleExecuteStatementErrors(e);
        }

        return result;
    }

    private List<SESEventsEntity> toList(ExecuteStatementResponse response) {
        return response.items().stream().map(item -> SESEventsEntity.builder()
                .sesMessageId(getStringValue(item, "SESMessageId"))
                .snsPublishTime(getStringValue(item, "SnsPublishTime"))
                .destinationEmail(getStringValue(item, "DestinationEmail"))
                .eventType(getStringValue(item, "EventType"))
                .message(Optional.ofNullable(item.get("Message"))
                        .filter(av -> av.m() != null)
                        .map(av -> convertAttributeValueMap2JsonString(av.m()))
                        .orElse(""))
                .customTag(getStringValue(item, "CustomTag"))
                .build()
        ).collect(Collectors.toList());
    }

    private String getStringValue(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key))
                .map(AttributeValue::s)
                .orElse("");
    }

    private String convertAttributeValueMap2JsonString(Map<String, AttributeValue> attributeValueMap) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(attributeValueMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleExecuteStatementErrors(Exception exception) {
        if (exception instanceof ConditionalCheckFailedException ccfe) {
            log.error("Condition check specified in the operation failed, review and update the condition " +
                    "check before retrying. Error: {}", ccfe.getMessage());
        } else if (exception instanceof TransactionConflictException tce) {
            log.error("Operation was rejected because there is an ongoing transaction for the item, generally " +
                    "safe to retry with exponential back-off. Error: {}", tce.getMessage());
        } else if (exception instanceof ProvisionedThroughputExceededException ptee) {
            log.error("Request rate is too high. Consider reducing frequency of requests or increasing provisioned capacity. Error: {}",
                    ptee.getMessage());
        } else if (exception instanceof ResourceNotFoundException rnfe) {
            log.error("One of the tables was not found, verify table exists before retrying. Error: {}", rnfe.getMessage());
        } else if (exception instanceof DynamoDbException dde) {
            log.error("DynamoDB error occurred. Error: {}", dde.getMessage());
        } else {
            log.error("An exception occurred. Error: {}", exception.getMessage());
        }
    }
}
