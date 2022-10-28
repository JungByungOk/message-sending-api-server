package com.msas.ses.repository;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msas.ses.model.SESEventsEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SESEventsDynamoDBRepository {

    private final AmazonDynamoDB amazonDynamoDB;

    public Optional<List<SESEventsEntity>> getItems()
    {
        List<SESEventsEntity> sesEventsEntityList = null;

        try {
            // Create ExecuteStatementRequest
            ExecuteStatementRequest executeStatementRequest = new ExecuteStatementRequest();
            executeStatementRequest.setStatement("select * from SESEvents;");
            ExecuteStatementResult executeStatementResult = amazonDynamoDB.executeStatement(executeStatementRequest);

            sesEventsEntityList = toList(executeStatementResult);

        } catch (Exception e) {
            handleExecuteStatementErrors(e);
        }

        return Optional.ofNullable(sesEventsEntityList);
    }

    public Optional<List<SESEventsEntity>> getItemsByCumstomTag(String CustomTag) {

        // TODO. 이메일 발송 결과를 가져와서 rdbms 이메일 이력 테이블에 상태 업데이트 처리 필요

        List<SESEventsEntity> sesEventsEntityList = null;

        try {
            // Create ExecuteStatementRequest
            ExecuteStatementRequest executeStatementRequest = new ExecuteStatementRequest();
            executeStatementRequest.setStatement(String.format("select * from SESEvents where CustomTag='%s';", CustomTag));

            ExecuteStatementResult executeStatementResult = amazonDynamoDB.executeStatement(executeStatementRequest);

            sesEventsEntityList = toList(executeStatementResult);

        } catch (Exception e) {
            handleExecuteStatementErrors(e);
        }

        return Optional.ofNullable(sesEventsEntityList);
    }

    public void deleteItemBySESMessageId(String SESMessageId)
    {
        // TODO. DaynamoDB 에서 이벤트 아이템을 삭제 구현
    }

    private List<SESEventsEntity> toList(ExecuteStatementResult executeStatementResult)
    {
        List<SESEventsEntity> sesEventsEntityList = new ArrayList<>();

        // Handle executeStatementResult
        executeStatementResult.getItems().forEach(new Consumer<Map<String, AttributeValue>>() {
            @Override
            public void accept(Map<String, AttributeValue> stringAttributeValueMap) {

                SESEventsEntity sesEventsEntity = SESEventsEntity.builder()
                        // partition-key
                        .sesMessageId(Optional.ofNullable(stringAttributeValueMap.get("SESMessageId")).map(AttributeValue::getS).orElse(""))
                        // sort-key
                        .snsPublishTime(Optional.ofNullable(stringAttributeValueMap.get("SnsPublishTime")).map(AttributeValue::getS).orElse(""))
                        .destinationEmail(Optional.ofNullable(stringAttributeValueMap.get("DestinationEmail")).map(AttributeValue::getS).orElse(""))
                        .eventType(Optional.ofNullable(stringAttributeValueMap.get("EventType")).map(AttributeValue::getS).orElse(""))
                        //.message(Optional.ofNullable(stringAttributeValueMap.get("Message")).map(AttributeValue::getM).map(p->new GsonBuilder().create().toJson(p)).orElse(""))   //gson->warning
                        .message(Optional.ofNullable(stringAttributeValueMap.get("Message")).map(AttributeValue::getM).map(p->convertAttributeValueMap2JsonString(p)).orElse(""))     //jackson->ok
                        .customTag(Optional.ofNullable(stringAttributeValueMap.get("CustomTag")).map(AttributeValue::getS).orElse(""))
                        .build();

                Map<String, AttributeValue> attributeValueMap = Optional.ofNullable(stringAttributeValueMap.get("Message")).map(AttributeValue::getM).orElse(new HashMap<>());

                sesEventsEntityList.add(sesEventsEntity);
            }
        });

        return sesEventsEntityList;
    }

    /**
     * DynamoDB AttributeValue 를 Json Serialize
     * Gson -> error 발생
     *      WARNING: An illegal reflective access operation has occurred
     *      WARNING: Illegal reflective access by com.google.gson.internal.reflect.ReflectionHelper (file:/C:/Users/jbo25/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10/dd9b193aef96e973d5a11ab13cd17430c2e4306b/gson-2.10.jar) to field java.nio.ByteBuffer.hb
     *      WARNING: Please consider reporting this to the maintainers of com.google.gson.internal.reflect.ReflectionHelper
     *      WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
     *      WARNING: All illegal access operations will be denied in a future release
     * Jackson -> 정상
     */
    private String convertAttributeValueMap2JsonString(Map<String, AttributeValue> attributeValueMap)
    {
        String strJson = "";
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            strJson = mapper.writeValueAsString(attributeValueMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return strJson;
    }

    // Handles errors during ExecuteStatement execution. Use recommendations in error messages below to add error handling specific to
    // your application use-case.
    private static void handleExecuteStatementErrors(Exception exception) {
        try {
            throw exception;
        } catch (ConditionalCheckFailedException ccfe) {
            log.error("Condition check specified in the operation failed, review and update the condition " +
                    "check before retrying. Error: " + ccfe.getErrorMessage());
        } catch (TransactionConflictException tce) {
            log.error(("Operation was rejected because there is an ongoing transaction for the item, generally " +
                    "safe to retry with exponential back-off. Error: " + tce.getErrorMessage()));
        } catch (ItemCollectionSizeLimitExceededException icslee) {
            log.error(("An item collection is too large, you\'re using Local Secondary Index and exceeded " +
                    "size limit of items per partition key. Consider using Global Secondary Index instead. Error: " + icslee.getErrorMessage()));
        } catch (Exception e) {
            handleCommonErrors(e);
        }
    }

    private static void handleCommonErrors(Exception exception) {
        try {
            throw exception;
        } catch (InternalServerErrorException isee) {
            log.error(("Internal Server Error, generally safe to retry with exponential back-off. Error: " + isee.getErrorMessage()));
        } catch (RequestLimitExceededException rlee) {
            log.error(("Throughput exceeds the current throughput limit for your account, increase account level throughput before " +
                    "retrying. Error: " + rlee.getErrorMessage()));
        } catch (ProvisionedThroughputExceededException ptee) {
            log.error(("Request rate is too high. If you're using a custom retry strategy make sure to retry with exponential back-off. " +
                    "Otherwise consider reducing frequency of requests or increasing provisioned capacity for your table or secondary index. Error: " +
                    ptee.getErrorMessage()));
        } catch (ResourceNotFoundException rnfe) {
            log.error(("One of the tables was not found, verify table exists before retrying. Error: " + rnfe.getErrorMessage()));
        } catch (AmazonServiceException ase) {
            log.error(("An AmazonServiceException occurred, indicates that the request was correctly transmitted to the DynamoDB " +
                    "service, but for some reason, the service was not able to process it, and returned an error response instead. Investigate and " +
                    "configure retry strategy. Error type: " + ase.getErrorType() + ". Error message: " + ase.getErrorMessage()));
        } catch (AmazonClientException ace) {
            log.error(("An AmazonClientException occurred, indicates that the client was unable to get a response from DynamoDB " +
                    "service, or the client was unable to parse the response from the service. Investigate and configure retry strategy. "+
                    "Error: " + ace.getMessage()));
        } catch (Exception e) {
            System.out.println("An exception occurred, investigate and configure retry strategy. Error: " + e.getMessage());
        }
    }

}
