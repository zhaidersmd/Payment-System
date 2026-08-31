package com.paymentplatform.payment.service;

import com.paymentplatform.payment.dto.PaymentBatchSummary;
import com.paymentplatform.payment.entity.Payment;

import java.math.BigDecimal;

public class PaymentSummaryAccumulator {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("25000");
    private long totalPayments = 0;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal highValueAmount = BigDecimal.ZERO;
    private long highValuePaymentCount = 0;

    public void add(Payment payment) {

        totalPayments++;
        totalAmount = totalAmount.add(payment.getAmount());
        if (payment.getAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            highValuePaymentCount++;
            highValueAmount = highValueAmount.add(payment.getAmount());
        }
    }

    public void combine(PaymentSummaryAccumulator other) {
        this.totalPayments += other.totalPayments;
        this.totalAmount = this.totalAmount.add(other.totalAmount);
        this.highValuePaymentCount += other.highValuePaymentCount;
        this.highValueAmount = this.highValueAmount.add(other.highValueAmount);
    }

    public PaymentBatchSummary toSummary() {
        return new PaymentBatchSummary(totalPayments, totalAmount, highValueAmount, highValuePaymentCount);
    }

}
