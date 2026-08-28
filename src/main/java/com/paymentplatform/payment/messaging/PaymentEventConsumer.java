package com.paymentplatform.payment.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventConsumer {

    @KafkaListener(topics = "payment-events", groupId = "payment-service")
    public void consume(String message) {
        System.out.println("Received payment event: " + message);
        //throw new RuntimeException( "TEST: consumer failure");
    }
}
