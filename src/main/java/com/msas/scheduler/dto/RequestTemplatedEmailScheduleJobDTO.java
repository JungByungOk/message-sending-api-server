package com.msas.scheduler.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RequestTemplatedEmailScheduleJobDTO {

    @NotNull
    @NotEmpty(message = "Email (from) sender cannot be Null")
    String from;

    @NotNull
    @NotEmpty(message = "Email templateName cannot be Null")
    String templateName;

    @NotNull
    @NotEmpty(message = "Email tags (campaign name or event name) cannot be Null")
    List<com.amazonaws.services.simpleemail.model.MessageTag> tags;

    private String jobGroup = "DEFAULT";

    @NotNull
    @NotEmpty(message = "JobName can't not be null.")
    private String jobName;

    private String description;

    @Future(message = "Invalid scheduling datetime. The past time cannot be set.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateAt;

    @NotNull(message = "TemplatedEmailList can't not be null.")
    private List<TemplatedEmailDto> templatedEmailList;


    @Data
    public static class TemplatedEmailDto {

        @NotNull
        @NotEmpty(message = "Email (to) receivers cannot be Null")
        List<String> to;

        List<String> cc;

        List<String> bcc;

        @NotNull
        @NotEmpty(message = "Email template data cannot be Null")
        Map<String, String> templateData;

    }


    public static class MessageTag extends com.amazonaws.services.simpleemail.model.MessageTag {
        private String name;
        private String value;
    }

}
