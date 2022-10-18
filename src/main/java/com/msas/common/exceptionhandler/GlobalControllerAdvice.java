package com.msas.common.exceptionhandler;

import com.amazonaws.Response;
import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException;
import com.msas.telegram.service.TelegramBotService;
import com.pengrad.telegrambot.TelegramException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 콘트롤러 전역에서 예외를 처리하고 사용자에게 에러 응답
 */
@RestControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

    /**
     * Controller RequestBody 를 @Valid 어노테이션을 통해 검증하고 검증 실패시 예외 처리 응답 한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors()
                .forEach(c -> errors.put(((FieldError) c).getField(), c.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * AWS SES 예외 처리 및 에러 응답
     * {
     * "errorType": "Client",
     * "errorMessage": "Email address is not verified. The following identities failed the check in region AP-NORTHEAST-2: no1-reply@nftreally.io",
     * "errorCode": "MessageRejected",
     * "serviceName": "AmazonSimpleEmailService"
     * }
     */
    @ExceptionHandler(AmazonSimpleEmailServiceException.class)
    public ResponseEntity<ResponseErrorDTO> handleAmazonSimpleEmailServiceExceptions(AmazonSimpleEmailServiceException ex) {

        ResponseErrorDTO responseErrorDTO = ResponseErrorDTO.builder()
                .serviceName(ex.getServiceName())
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType().name())
                .errorMessage(ex.getErrorMessage())
                .build();

        return new ResponseEntity<>(responseErrorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SchedulerException.class)
    public ResponseEntity<ResponseErrorDTO> handleScheduleExceptions(SchedulerException ex) {

        ResponseErrorDTO responseErrorDTO = ResponseErrorDTO.builder()
                .serviceName("Scheduler(Quartz)")
                .errorType(ex.getClass().getTypeName())
                .errorMessage(ex.getMessage())
                .build();

        return new ResponseEntity<>(responseErrorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TelegramException.class)
    public ResponseEntity<ResponseErrorDTO> handleTelegramServiceExceptions(TelegramException ex) {

        ResponseErrorDTO responseErrorDTO = ResponseErrorDTO.builder()
                .serviceName("TelegramBot")
                .errorType(ex.getClass().getTypeName())
                .errorMessage(ex.getMessage())
                .build();

        return new ResponseEntity<>(responseErrorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
