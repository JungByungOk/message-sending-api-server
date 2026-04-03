package com.msas.ses.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

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

    final String subscriptionString = "SubscriptionConfirmation";

    @PostMapping("/bounces")
    public void bounce(@RequestBody String body, HttpServletRequest request) {
        if (body.contains(subscriptionString))
            log.info("Bounces - SubscriptionConfirmation\n{}\n", body);
    }

    @PostMapping("/complaints")
    public void complaints(@RequestBody String body, HttpServletRequest request) {
        if (body.contains(subscriptionString))
            log.info("Complaints SubscriptionConfirmation\n{}\n", body);
    }

    @PostMapping("/deliveries")
    public void delivery(@RequestBody String body, HttpServletRequest request) {
        if (body.contains(subscriptionString))
            log.info("Deliveries - SubscriptionConfirmation\n{}\n", body);
    }

    @PostMapping("/events")
    public void event(@RequestBody String body, HttpServletRequest request) {
        if (body.contains(subscriptionString))
            log.info("Events - SubscriptionConfirmation\n{}\n", body);
    }
}
