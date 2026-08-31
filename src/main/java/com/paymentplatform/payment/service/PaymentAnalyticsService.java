package com.paymentplatform.payment.service;

import com.paymentplatform.payment.dto.PaymentAnalyticsItem;
import com.paymentplatform.payment.dto.PaymentBatchSummary;
import com.paymentplatform.payment.dto.PaymentSummaryResponse;
import com.paymentplatform.payment.entity.Payment;
import com.paymentplatform.payment.entity.PaymentStatus;
import com.paymentplatform.payment.projection.PaymentAggregate;
import com.paymentplatform.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentAnalyticsService {

    private final PaymentRepository paymentRepository;
    private static final BigDecimal HIGH_VALUE_PAYMENT =
            new BigDecimal("25000");

    public PaymentAnalyticsService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentSummaryResponse getCustomerPaymentSummary(String customerId) {
        PaymentAggregate aggregate = paymentRepository
                .getPaymentAggregateByCustomerId(customerId);

        return new PaymentSummaryResponse(
                customerId,
                aggregate.getTotalPayments(),
                aggregate.getTotalAmount(),
                aggregate.getAveragePaymentAmount(),
                null,
                null,
                null,
                null
        );
    }

    public PaymentBatchSummary getBatchSummary(
            String customerId) {

        List<Payment> payments =
                paymentRepository.findByCustomerId(customerId);
        long start = System.currentTimeMillis();
        PaymentBatchSummary result = payments.parallelStream()
                .collect(
                        PaymentBatchSummaryCollector
                                .toPaymentBatchSummary()
                );
        long end = System.currentTimeMillis();
        System.out.println( "Sequential processing time: " + (end - start) + " ms");
        return result;
    }

}
