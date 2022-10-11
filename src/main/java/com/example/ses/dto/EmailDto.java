package com.example.ses.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@Data
public class EmailDto {

    @Email(message = "Invalid Email sender address")
    String fromEmail;

    @Email(message = "Invalid Email recipient address")
    String toEmail;

    @NotEmpty(message = "Email subject cannot be Null")
    String subject;

    @NotEmpty(message = "Email body cannot be Null")
    String body;

}
