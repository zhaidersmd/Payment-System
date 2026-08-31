package com.paymentplatform.payment.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentEventConsumer {

    @KafkaListener(topics = "payment-events", groupId = "payment-service")
    public void consume(String message) {
        log.info("Received payment event: " + message);

        //throw new RuntimeException( "TEST: consumer failure");
    }
}
