package com.paymentplatform.payment.processor;

import com.paymentplatform.payment.dto.v2.CreatePaymentRequestV2;
import com.paymentplatform.payment.entity.PaymentMethod;


public interface PaymentProcessor {

    PaymentMethod getPaymentMethod();
    void validate(CreatePaymentRequestV2 request);
}
