package com.msas.scheduler.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class RequestScheduleDTO {
    private String jobGroup = "DEFAULT";

    @NotNull
    @NotEmpty(message = "JobName can't not be null.")
    private String jobName;
}
