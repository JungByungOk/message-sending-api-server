package com.msas.ses.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class RequestDeleteTemplateDto {

    @NotNull
    @NotEmpty(message = "Email templateName cannot be Null")
    String templateName;

}
