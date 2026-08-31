package com.paymentplatform.payment.dto;

import com.paymentplatform.payment.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PaymentSummaryResponse (
        String customerId,
        long totalPayments,
        BigDecimal totalAmount,
        BigDecimal averagePaymentAmount,
        Map<PaymentStatus, Long> paymentsByStatus,
        Map<String, BigDecimal> amountByCurrency,
        List<PaymentAnalyticsItem> topPayments,
        Map<Boolean, Long> paymentValueDistribution) {
}
