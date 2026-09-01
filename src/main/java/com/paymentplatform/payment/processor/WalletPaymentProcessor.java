package com.paymentplatform.payment.processor;

import com.paymentplatform.payment.dto.v2.CreatePaymentRequestV2;
import com.paymentplatform.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class WalletPaymentProcessor implements PaymentProcessor{
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.WALLET;
    }

    @Override
    public void validate(CreatePaymentRequestV2 request) {
        if (request.getAmount().compareTo( new java.math.BigDecimal("10000") ) > 0) {
            throw new IllegalArgumentException(
                    "Wallet payment cannot exceed 10000"
            );
        }
    }
}
