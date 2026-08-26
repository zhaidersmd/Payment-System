package com.paymentplatform.payment.exception;

import org.springframework.stereotype.Component;

import java.util.UUID;

public class PaymentNotFoundException
        extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found: " + paymentId);
    }
}
