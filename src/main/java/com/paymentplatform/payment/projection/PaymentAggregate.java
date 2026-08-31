package com.paymentplatform.payment.projection;

import java.math.BigDecimal;

public interface PaymentAggregate {
    Long getTotalPayments();
    BigDecimal getTotalAmount();
    BigDecimal getAveragePaymentAmount();
}