package com.msas.ses.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@Getter
@Setter
@DynamoDBTable(tableName = "SESEvents")
public class SESEventsEntity {

    @DynamoDBHashKey(attributeName = "SESMessageId")
    private String SESMessageId;        //partition-key: hash

    @DynamoDBRangeKey(attributeName = "SnsPublishTime")
    private String SnsPublishTime;      //sort-key: range

    @DynamoDBIndexHashKey(attributeName = "DestinationEmail")
    private String DestinationEmail;    //index - partition-key: string

    @DynamoDBAttribute
    private String LambdaReceiveTime;

    @DynamoDBAttribute
    private String EventType;

    @DynamoDBAttribute
    private String Message;

    @Builder
    public SESEventsEntity(String SESMessageId, String snsPublishTime, String destinationEmail, String lambdaReceiveTime, String eventType, String message) {
        this.SESMessageId = SESMessageId;
        SnsPublishTime = snsPublishTime;
        DestinationEmail = destinationEmail;
        LambdaReceiveTime = lambdaReceiveTime;
        EventType = eventType;
        Message = message;
    }
}
