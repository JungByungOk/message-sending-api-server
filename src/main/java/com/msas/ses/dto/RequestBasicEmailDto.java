package com.msas.ses.dto;

import com.amazonaws.services.simpleemail.model.MessageTag;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class RequestBasicEmailDto {

    @NotNull
    @Email(message = "Invalid Email sender address")
    String from;

    @NotNull
    @Email(message = "Invalid Email recipient address")
    String to;

    @NotNull
    @NotEmpty(message = "Email subject cannot be Null")
    String subject;

    @NotNull
    @NotEmpty(message = "Email body cannot be Null")
    String body;

    @NotNull
    @NotEmpty(message = "Email tags (campaign name or event name) cannot be Null")
    List<MessageTag> tags;
}
