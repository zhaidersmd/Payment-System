package com.paymentplatform.payment.service;

import com.paymentplatform.payment.entity.OutboxEvent;
import com.paymentplatform.payment.entity.OutboxEventStatus;
import com.paymentplatform.payment.messaging.KafkaEventPublisher;
import com.paymentplatform.payment.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
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

            int claimed = outboxEventRepository.claimEvent(
                    event.getId(),
                    OutboxEventStatus.PENDING,
                    OutboxEventStatus.PROCESSING,
                    LocalDateTime.now());

            if (claimed == 0) {
                continue;
            }




            try {
                kafkaEventPublisher.publish(
                        event.getAggregateId().toString(),
                        event.getPayload());

                event.setStatus(
                        OutboxEventStatus.PUBLISHED);

                event.setPublishedAt(
                        LocalDateTime.now());

                event.setProcessingStartedAt(null);

                outboxEventRepository.save(event);
            }catch (Exception e) {

                event.setStatus( OutboxEventStatus.PENDING);
                event.setProcessingStartedAt(null);
                event.setRetryCount( event.getRetryCount() + 1);
                outboxEventRepository.save(event);

            }
        }
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void recoverStuckEvents() {

        LocalDateTime cutoff =
                LocalDateTime.now().minusMinutes(1);

        int recovered =
                outboxEventRepository
                        .recoverStuckEvents(
                                OutboxEventStatus.PROCESSING,
                                OutboxEventStatus.PENDING,
                                cutoff);

        System.out.println("Recovered = " + recovered);
        List<OutboxEvent> events =
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                OutboxEventStatus.PENDING);

        events.forEach(event ->
                System.out.println(
                        "EVENT = " + event.getId()
                                + " STATUS = " + event.getStatus()
                                + " PROCESSING_STARTED = "
                                + event.getProcessingStartedAt()
                )
        );

        if (recovered > 0) {
            System.out.println(
                    "Recovered " + recovered
                            + " stuck outbox events");
        }
    }
}
