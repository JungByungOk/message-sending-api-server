package com.msas.ses.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    List<MessageTagDto> tags;
}
