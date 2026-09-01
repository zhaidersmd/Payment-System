package com.paymentplatform.payment.processor;

import com.paymentplatform.payment.dto.v2.CreatePaymentRequestV2;
import com.paymentplatform.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentProcessor implements PaymentProcessor{
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public void validate(CreatePaymentRequestV2 request) {
        if (request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Card payment amount must be greater than zero");
        }
    }
}
