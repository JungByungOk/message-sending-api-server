package com.msas.pollingchecker.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SESEventsEntity {

    private String SesMessageId;        //partition-key: hash

    private String SnsPublishTime;      //sort-key: range

    private String DestinationEmail;    //index - partition-key: string

    public String getEventType() {
        EventType = EventType.replace(" ", "");
        return EventType;
    }

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
