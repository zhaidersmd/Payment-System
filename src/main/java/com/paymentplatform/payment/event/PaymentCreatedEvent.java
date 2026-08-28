package com.paymentplatform.payment.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PaymentCreatedEvent (
        UUID paymentId,
        String customerId,
        BigDecimal amount,
        String currency

) {


}
