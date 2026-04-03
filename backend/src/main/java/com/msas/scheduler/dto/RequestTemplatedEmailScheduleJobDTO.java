package com.msas.scheduler.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
            "templateParameters": {
                "user_name": "Alyssa"
            }
        },
        {
            "to": [
                "jbo2541@gmail.com"
            ],
            "templateParameters": {
                "user_name": "Angela"
            }
        },
        {
            "to": [
                "jbo2541@gmail.com"
            ],
            "templateParameters": {
                "user_name": "Emma"
            }
        },
        {
            "to": [
                "jbo2541@gmail.com"
            ],
            "templateParameters": {
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
    List<com.msas.ses.dto.MessageTagDto> tags;

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

        // 이메일 건당 식별
        // id = aws ses messageId 맵핑하여 기록 추적용
        String id;

        @NotNull
        @NotEmpty(message = "Email (to) receivers cannot be Null")
        List<String> to;

        List<String> cc;

        List<String> bcc;

        @NotNull
        @NotEmpty(message = "Email template data cannot be Null")
        Map<String, String> templateParameters;

    }

}
