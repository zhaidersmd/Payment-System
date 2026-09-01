package com.paymentplatform.payment.processor;

import com.paymentplatform.payment.entity.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PaymentProcessorResolver {

    private final Map<PaymentMethod, PaymentProcessor> processors;

    public PaymentProcessorResolver(List<PaymentProcessor> paymentProcessors) {

        Map<PaymentMethod, PaymentProcessor> processorMap = new EnumMap<>(PaymentMethod.class);

        for (PaymentProcessor processor : paymentProcessors) {

            PaymentProcessor existing =processorMap.put(processor.getPaymentMethod(),processor );

            if (existing != null) {
                throw new IllegalStateException( "Multiple processors found for payment method: "
                                + processor.getPaymentMethod()
                );
            }
        }

        this.processors = Map.copyOf(processorMap);
    }

    public PaymentProcessor resolve(PaymentMethod paymentMethod) {

        PaymentProcessor processor = processors.get(paymentMethod);

        if (processor == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod
            );
        }

        return processor;
    }
}
