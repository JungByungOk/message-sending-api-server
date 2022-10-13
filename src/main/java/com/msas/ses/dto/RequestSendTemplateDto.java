package com.msas.ses.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
public class RequestSendTemplateDto {

    @NotNull
    @NotEmpty(message = "Email templateName cannot be Null")
    String templateName;

    @NotNull
    @NotEmpty(message = "Email (from) sender cannot be Null")
    String from;

    @NotNull
    @NotEmpty(message = "Email (to) receivers cannot be Null")
    List<String> to;

    List<String> cc;

    List<String> bcc;

    @NotNull
    @NotEmpty(message = "Email template data cannot be Null")
    Map<String, String> templateData;

}
