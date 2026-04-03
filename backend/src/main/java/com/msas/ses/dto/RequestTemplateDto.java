package com.msas.ses.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class RequestTemplateDto {

    @NotNull
    @NotEmpty(message = "Email templateName cannot be Null")
    String templateName;

    @NotNull
    @NotEmpty(message = "Email subjectPart cannot be Null")
    String subjectPart;

    @NotNull
    @NotEmpty(message = "Email htmlPart cannot be Null")
    String htmlPart;

    @NotNull
    @NotEmpty(message = "Email textPart cannot be Null")
    String textPart;

}
