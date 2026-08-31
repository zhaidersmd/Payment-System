package com.paymentplatform.payment.dto;

import java.math.BigDecimal;

public record PaymentBatchSummary (


        long totalPayments,

        BigDecimal totalAmount,

        BigDecimal highValueAmount,

        long highValuePaymentCount
) {
}
