package com.msas.ses.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * <Notification Types>
 * SES Feedback Types
 * Bounce 이메일은 받는 사람 이메일이 없는 경우
 * Complaints 이메일은 수신자가 받은 편지함에서 차단한 경우
 * Delivery: 이메일 전송이 성공한 경우
 * --------------------------------------------------
 * <Event Types>
 * Send
 * Rendering failures
 * Rejects
 * Deliveries
 * Hard bounces
 * Complaints
 * Delivery delays
 * Subscriptions
 * ------------------- Open and click tracking
 * Opens
 * Clicks
 */
@RestController
@RequestMapping("/ses/feedback")
@RequiredArgsConstructor
@Slf4j
public class SESFeedbackNotificationController {

    // TODO: SES 통지 데이터를 데이타베이스에 저장하도록 구현해야 한다. -> 통지 데이터를 통해 집계를 해야한다.

    @PostMapping("/bounces")
    public void bounce(@RequestBody String body, HttpServletRequest request) {
        if (body.contains("SubscriptionConfirmation"))
            log.info("Bounces - SubscriptionConfirmation\n{}\n", body);
    }

    @PostMapping("/complaints")
    public void complaints(@RequestBody String body, HttpServletRequest request) {
        if (body.contains("SubscriptionConfirmation"))
            log.info("Complaints SubscriptionConfirmation\n{}\n", body);
    }

    @PostMapping("/deliveries")
    public void delivery(@RequestBody String body, HttpServletRequest request) {
        if (body.contains("SubscriptionConfirmation"))
            log.info("Deliveries - SubscriptionConfirmation\n{}\n", body);
    }

    @PostMapping("/events")
    public void event(@RequestBody String body, HttpServletRequest request) {
        if (body.contains("SubscriptionConfirmation"))
            log.info("Events - SubscriptionConfirmation\n{}\n", body);
    }
}
