package com.msas.ses.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@Data
public class SESEventsEntity {

    private String SESMessageId;        //partition-key: hash

    private String SnsPublishTime;      //sort-key: range

    private String DestinationEmail;    //index - partition-key: string

    private String EventType;

    private String Message;

    @Builder
    public SESEventsEntity(String SESMessageId, String snsPublishTime, String destinationEmail, String eventType, String message) {
        this.SESMessageId = SESMessageId;
        SnsPublishTime = snsPublishTime;
        DestinationEmail = destinationEmail;
        EventType = eventType;
        Message = message;
    }
}
