package com.msas.scheduler.dto;

import com.amazonaws.services.simpleemail.model.MessageTag;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
public class TemplatedEmailDto {

    @NotNull
    @NotEmpty(message = "Email (to) receivers cannot be Null")
    List<String> to;

    List<String> cc;

    List<String> bcc;

    @NotNull
    @NotEmpty(message = "Email template data cannot be Null")
    Map<String, String> templateData;

}
