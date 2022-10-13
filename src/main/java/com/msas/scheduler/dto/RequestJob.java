package com.msas.scheduler.dto;

import lombok.Data;
import org.quartz.JobDataMap;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class RequestJob {
    private String jobGroup = "DEFAULT";

    @NotNull
    @NotEmpty(message = "jobName can't not be null.")
    private String jobName;

    @Future(message = "invalid datetime.")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDateAt;

    @Digits(message = "invalid repeatIntervalInSeconds.", integer = 0, fraction = 0)
    private long repeatIntervalInSeconds;
    @Digits(message = "invalid repeatCount", integer = 0, fraction = 0)
    private int repeatCount;

    private String cronExpression;
    private JobDataMap jobDataMap;
}
