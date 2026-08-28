package com.paymentplatform.payment.service;

import com.paymentplatform.payment.entity.OutboxEvent;
import com.paymentplatform.payment.entity.OutboxEventStatus;
import com.paymentplatform.payment.messaging.KafkaEventPublisher;
import com.paymentplatform.payment.repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaEventPublisher kafkaEventPublisher) {

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                OutboxEventStatus.PENDING);

        for (OutboxEvent event : events) {

            try {
                kafkaEventPublisher.publish(
                        event.getAggregateId().toString(),
                        event.getPayload());

                event.setStatus(
                        OutboxEventStatus.PUBLISHED);

                event.setPublishedAt(
                        LocalDateTime.now());

                outboxEventRepository.save(event);
            }catch (Exception e) {
                event.setRetryCount(
                        event.getRetryCount() + 1);

                outboxEventRepository.save(event);

            }
        }
    }
}
