package com.paymentplatform.payment.dto;

import com.paymentplatform.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentAnalyticsItem(

        UUID paymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status
) {
}
