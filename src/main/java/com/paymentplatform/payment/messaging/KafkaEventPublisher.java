package com.paymentplatform.payment.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class KafkaEventPublisher {
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(
            String key,
            String payload) {

        try {
            kafkaTemplate.send(
                    PAYMENT_EVENTS_TOPIC,
                    key,
                    payload).get();
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException( "Kafka publishing was interrupted", e);

        }catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish event to Kafka", e);
        }
    }
}
