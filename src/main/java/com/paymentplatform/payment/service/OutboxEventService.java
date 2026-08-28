package com.paymentplatform.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.entity.OutboxEvent;
import com.paymentplatform.payment.entity.OutboxEventStatus;
import com.paymentplatform.payment.event.PaymentCreatedEvent;
import com.paymentplatform.payment.repository.OutboxEventRepository;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;


    public OutboxEventService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void createPaymentCreatedEvent(
            UUID paymentId, String customerId, BigDecimal amount, String currency ) {
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(paymentId)
                .amount(amount)
                .customerId(customerId)
                .currency(currency).build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(paymentId)
                    .aggregateType("Payment")
                    .eventType("PaymentCreated")
                    .payload(payload)
                    .status(OutboxEventStatus.PENDING)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException( "Failed to serialize PaymentCreated event", e);
        }
    }
}
