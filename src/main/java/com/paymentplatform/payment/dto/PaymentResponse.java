package com.paymentplatform.payment.dto;

import com.paymentplatform.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        String customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        LocalDateTime createdAt
) {

}
