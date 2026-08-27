package com.paymentplatform.payment.exception;

public class IdempotencySerializationException
        extends RuntimeException {

    public IdempotencySerializationException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
