package com.msas.common;

import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException;
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
    public ResponseEntity<Map<String, String>> handleAmazonSimpleEmailServiceExceptions(AmazonSimpleEmailServiceException ex) {

        Map<String, String> errors = new HashMap<>();
        {
            errors.put("errorCode", ex.getErrorCode());
            errors.put("errorType", ex.getErrorType().name());
            errors.put("errorMessage", ex.getErrorMessage());
            errors.put("serviceName", ex.getServiceName());
        }

        return ResponseEntity.badRequest().body(errors);
    }

}
