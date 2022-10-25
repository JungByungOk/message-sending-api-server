package com.msas.ses.model;

import lombok.*;

@Data
public class SESEventsEntity {

    private String SesMessageId;        //partition-key: hash

    private String SnsPublishTime;      //sort-key: range

    private String DestinationEmail;    //index - partition-key: string

    private String EventType;

    private String Message;

    private String CustomTag;

    @Builder
    public SESEventsEntity(String sesMessageId, String snsPublishTime, String destinationEmail, String eventType, String message, String customTag) {
        SesMessageId = sesMessageId;
        SnsPublishTime = snsPublishTime;
        DestinationEmail = destinationEmail;
        EventType = eventType;
        Message = message;
        CustomTag = customTag;
    }
}
