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

    public List<SESEventsEntity> getItems(String CustomTag) {

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
                                    .SESMessageId(stringAttributeValueMap.get("SESMessageId").getS())
                                    .eventType(stringAttributeValueMap.get("EventType").getS())

                            .build());
                }
            });

        } catch (Exception e) {
            handleExecuteStatementErrors(e);
        }

        return sesEventsEntityList;
    }

    // Handles errors during ExecuteStatement execution. Use recommendations in error messages below to add error handling specific to
    // your application use-case.
    private static void handleExecuteStatementErrors(Exception exception) {
        try {
            throw exception;
        } catch (ConditionalCheckFailedException ccfe) {
            System.out.println("Condition check specified in the operation failed, review and update the condition " +
                    "check before retrying. Error: " + ccfe.getErrorMessage());
        } catch (TransactionConflictException tce) {
            System.out.println("Operation was rejected because there is an ongoing transaction for the item, generally " +
                    "safe to retry with exponential back-off. Error: " + tce.getErrorMessage());
        } catch (ItemCollectionSizeLimitExceededException icslee) {
            System.out.println("An item collection is too large, you\'re using Local Secondary Index and exceeded " +
                    "size limit of items per partition key. Consider using Global Secondary Index instead. Error: " + icslee.getErrorMessage());
        } catch (Exception e) {
            handleCommonErrors(e);
        }
    }

    private static void handleCommonErrors(Exception exception) {
        try {
            throw exception;
        } catch (InternalServerErrorException isee) {
            System.out.println("Internal Server Error, generally safe to retry with exponential back-off. Error: " + isee.getErrorMessage());
        } catch (RequestLimitExceededException rlee) {
            System.out.println("Throughput exceeds the current throughput limit for your account, increase account level throughput before " +
                    "retrying. Error: " + rlee.getErrorMessage());
        } catch (ProvisionedThroughputExceededException ptee) {
            System.out.println("Request rate is too high. If you're using a custom retry strategy make sure to retry with exponential back-off. " +
                    "Otherwise consider reducing frequency of requests or increasing provisioned capacity for your table or secondary index. Error: " +
                    ptee.getErrorMessage());
        } catch (ResourceNotFoundException rnfe) {
            System.out.println("One of the tables was not found, verify table exists before retrying. Error: " + rnfe.getErrorMessage());
        } catch (AmazonServiceException ase) {
            System.out.println("An AmazonServiceException occurred, indicates that the request was correctly transmitted to the DynamoDB " +
                    "service, but for some reason, the service was not able to process it, and returned an error response instead. Investigate and " +
                    "configure retry strategy. Error type: " + ase.getErrorType() + ". Error message: " + ase.getErrorMessage());
        } catch (AmazonClientException ace) {
            System.out.println("An AmazonClientException occurred, indicates that the client was unable to get a response from DynamoDB " +
                    "service, or the client was unable to parse the response from the service. Investigate and configure retry strategy. "+
                    "Error: " + ace.getMessage());
        } catch (Exception e) {
            System.out.println("An exception occurred, investigate and configure retry strategy. Error: " + e.getMessage());
        }
    }

}
