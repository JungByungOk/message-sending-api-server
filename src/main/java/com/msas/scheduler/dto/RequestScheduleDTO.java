package com.msas.scheduler.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class RequestScheduleDTO {
    private String jobGroup = "DEFAULT";

    @NotNull
    @NotEmpty(message = "JobName can't not be null.")
    private String jobName;
}
