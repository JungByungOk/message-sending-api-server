package com.msas.scheduler.dto;

import com.amazonaws.services.simpleemail.model.MessageTag;
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

    private String jobGroup = "DEFAULT";

    @NotNull
    @NotEmpty(message = "JobName can't not be null.")
    private String jobName;

    private String description;

    @NotNull
    @NotEmpty(message = "Email (from) sender cannot be Null")
    String from;

    @Future(message = "Invalid scheduling datetime. The past time cannot be set.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateAt;

    @NotNull
    @NotEmpty(message = "Email templateName cannot be Null")
    String templateName;

    @NotNull(message = "TemplatedEmailList can't not be null.")
    private List<TemplatedEmailDto> templatedEmailList;

    @NotNull
    @NotEmpty(message = "Email tags (campaign name or event name) cannot be Null")
    List<MessageTag> tags;

}
