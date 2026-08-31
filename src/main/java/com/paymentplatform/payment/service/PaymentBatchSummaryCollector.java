package com.paymentplatform.payment.service;

import com.paymentplatform.payment.dto.PaymentBatchSummary;
import com.paymentplatform.payment.entity.Payment;

import java.util.stream.Collector;

public class PaymentBatchSummaryCollector {

    private PaymentBatchSummaryCollector() {
    }

    public static Collector<
            Payment,
            PaymentSummaryAccumulator,
            PaymentBatchSummary
            > toPaymentBatchSummary() {

        return Collector.of(

                PaymentSummaryAccumulator::new,

                PaymentSummaryAccumulator::add,

                (left, right) -> {
                    left.combine(right);
                    return left;
                },

                PaymentSummaryAccumulator::toSummary
        );
    }
}
