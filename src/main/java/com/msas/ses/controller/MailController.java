package com.msas.ses.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.msas.ses.dto.EmailDto;
import com.msas.ses.service.SESMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AWS SES 를 이용하여 이메일 전송 요청을 처리
 */
@RestController
@RequestMapping("/ses")
@RequiredArgsConstructor
public class MailController {

    private final SESMailService SESMailService;

    @PostMapping("/send")
    public String sendEmail(@RequestBody EmailDto emailDTO)
    {
        return new Gson().toJson(SESMailService.sendEmail(emailDTO));
    }

    public String sendEmailWithAttachment()
    {
        return "";
    }
}
