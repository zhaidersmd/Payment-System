package com.paymentplatform.payment.processor;

import com.paymentplatform.payment.dto.v2.CreatePaymentRequestV2;
import com.paymentplatform.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentProcessor implements PaymentProcessor{
    @Override
    public PaymentMethod getPaymentMethod() {

        return PaymentMethod.UPI;
    }

    @Override
    public void validate(CreatePaymentRequestV2 request) {
        if (!"INR".equalsIgnoreCase(request.getCurrency())) {
            throw new IllegalArgumentException( "UPI payments are supported only in INR");
        }
    }
}
