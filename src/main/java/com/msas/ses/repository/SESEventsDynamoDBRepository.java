package com.msas.ses.repository;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.msas.ses.model.SESEventsEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SESEventsDynamoDBRepository {

    private final AmazonDynamoDB amazonDynamoDB;

    public List<SESEventsEntity> getItemsByCumstomTag(String CustomTag) {

        // TODO. 이메일 발송 결과를 가져와서 rdbms 이메일 이력 테이블에 상태 업데이트 처리 필요

        List<SESEventsEntity> sesEventsEntityList = new ArrayList<>();

        try {
            // Create ExecuteStatementRequest
            ExecuteStatementRequest executeStatementRequest = new ExecuteStatementRequest();
            executeStatementRequest.setStatement(String.format("select * from SESEvents where CustomTag='%s';", CustomTag));

            ExecuteStatementResult executeStatementResult = amazonDynamoDB.executeStatement(executeStatementRequest);

            // Handle executeStatementResult
            executeStatementResult.getItems().forEach(new Consumer<Map<String, AttributeValue>>() {
                @Override
                public void accept(Map<String, AttributeValue> stringAttributeValueMap) {
                    sesEventsEntityList.add(SESEventsEntity.builder()
                                    .sesMessageId(stringAttributeValueMap.get("SESMessageId").getS())       // partition-key
                                    .snsPublishTime(stringAttributeValueMap.get("SnsPublishTime").getS())   // sort-key
                                    .destinationEmail(stringAttributeValueMap.get("DestinationEmail").getS())
                                    .eventType(stringAttributeValueMap.get("EventType").getS())
                                    .message(stringAttributeValueMap.get("Message").getS())
                                    .customTag(stringAttributeValueMap.get("CustomTag").getS())
                            .build());
                }
            });

        } catch (Exception e) {
            handleExecuteStatementErrors(e);
        }

        return sesEventsEntityList;
    }

    public void deleteItemBySESMessageId(String SESMessageId)
    {
        // TODO. DaynamoDB 에서 이벤트 아이템을 삭제 구현
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
