package com.msas.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
Sample
{
    "jobName": "job1",
    "jobGroup": "group1",
    "description": "스케쥴 작업 등록 테스트",
    "startDateAt": "2023-10-30T14:15:00",
    "templateName": "TEST-Template-Welcome",
    "from": "no-reply@nftreally.io",
    "templatedEmailList": [
        {
            "to": [
                "jbo2541@gmail.com"
            ],
            "templateData": {
                "user_name": "Alyssa"
            }
        },
        {
            "to": [
                "jbo2541@gmail.com"
            ],
            "templateData": {
                "user_name": "Angela"
            }
        },
        {
            "to": [
                "jbo2541@gmail.com"
            ],
            "templateData": {
                "user_name": "Emma"
            }
        },
        {
            "to": [
                "jbo2541@gmail.com"
            ],
            "templateData": {
                "user_name": "Lucy"
            }
        }
    ],
    "tags": [
        {
            "name": "customTag",
            "value": "20221025154600"
        }
    ]
}
 */

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

}
