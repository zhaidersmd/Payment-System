package com.paymentplatform.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handlePaymentNotFound(PaymentNotFoundException exception) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 404,
                "error", "PAYMENT_NOT_FOUND",
                "message", exception.getMessage()
        );

    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidPaymentState(InvalidPaymentStateException exception) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "INVALID_PAYMENT_STATE",
                "message", exception.getMessage()
        );

    }

}
