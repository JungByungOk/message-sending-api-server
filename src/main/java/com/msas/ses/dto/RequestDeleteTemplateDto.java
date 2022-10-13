package com.msas.ses.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class RequestDeleteTemplateDto {

    @NotNull
    @NotEmpty(message = "Email templateName cannot be Null")
    String templateName;

}
