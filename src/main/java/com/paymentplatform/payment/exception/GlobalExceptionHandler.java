package com.paymentplatform.payment.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;


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
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Map<String, Object> handleOptimisticLockingFailure(
            OptimisticLockingFailureException exception) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.CONFLICT,
                "error", "CONCURRENT_UPDATE",
                "message", "Payment was modified by another request"
        );
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public Map<String, Object> handleIdempotencyKeyConflictException(
            IdempotencyKeyConflictException exception) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.CONFLICT,
                "error", "IdempotencyKeyConflictException",
                "message", "Idempotency-Key has already been used with a different request"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.CONFLICT.value(),
                "error", "IDEMPOTENCY_CONFLICT",
                "message", "A request with this idempotency key is already being processed."
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleBadCredentialsException(BadCredentialsException exception) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "INVALID_CREDENTIALS",
                "message", "Invalid username or password"
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleUnexpectedException(Exception exception) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "INTERNAL_SERVER_ERROR",
                "message", "An unexpected error occurred"
        );
    }

}
