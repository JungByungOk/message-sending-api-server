package com.msas.ses.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class EmailDto {

    @NotNull
    @Email(message = "Invalid Email sender address")
    String fromEmail;

    @NotNull
    @Email(message = "Invalid Email recipient address")
    String toEmail;

    @NotNull
    @NotEmpty(message = "Email subject cannot be Null")
    String subject;

    @NotNull
    @NotEmpty(message = "Email body cannot be Null")
    String body;

}
