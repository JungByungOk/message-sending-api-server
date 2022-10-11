package com.msas.ses.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * SES Feedback Types
 * Bounce 이메일은 받는 사람 이메일이 없는 경우
 * Complaints 이메일은 수신자가 받은 편지함에서 차단한 경우
 * Delivery: 이메일 전송이 성공한 경우
 */
@RestController
@RequestMapping("/ses/feedback")
@RequiredArgsConstructor
public class SESFeedbackNotificationController {

    @PostMapping("/bounce")
    public void bounce(@RequestBody String body, HttpServletRequest request) {
        System.out.println("->" + request.getRequestURI());
        System.out.println(body);
    }

    @PostMapping("/complaints")
    public void complaints(@RequestBody String body, HttpServletRequest request) {
        System.out.println("->" + request.getRequestURI());
        System.out.println(body);
    }

    @PostMapping("/delivery")
    public void delivery(@RequestBody String body, HttpServletRequest request) {
        System.out.println("->" + request.getRequestURI());
        System.out.println(body);
    }

    @PostMapping("/event")
    public void event(@RequestBody String body, HttpServletRequest request) {
        System.out.println("->" + request.getRequestURI());
        System.out.println(body);
    }
}
