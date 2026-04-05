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
    @NotEmpty(message = "Email recipient list cannot be empty")
    List<String> to;

    @NotNull
    @NotEmpty(message = "Email subject cannot be Null")
    String subject;

    @NotNull
    @NotEmpty(message = "Email body cannot be Null")
    String body;

    List<MessageTagDto> tags;
}
