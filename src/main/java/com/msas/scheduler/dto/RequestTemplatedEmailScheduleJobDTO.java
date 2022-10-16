package com.msas.scheduler.dto;

import com.msas.ses.dto.RequestTemplatedEmailDto;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RequestTemplatedEmailScheduleJobDTO {

    @NotNull(message = "TemplatedEmailList can't not be null.")
    private List<RequestTemplatedEmailDto> templatedEmailList;
    
    private String jobGroup = "DEFAULT";

    @NotNull
    @NotEmpty(message = "JobName can't not be null.")
    private String jobName;

    private String description;

    @Future(message = "Invalid scheduling datetime. The past time cannot be set.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateAt;

}
