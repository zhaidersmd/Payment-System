package com.paymentplatform.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.dto.CreatePaymentRequest;
import com.paymentplatform.payment.dto.PaymentResponse;
import com.paymentplatform.payment.entity.IdempotencyRecord;
import com.paymentplatform.payment.entity.Payment;
import com.paymentplatform.payment.entity.PaymentStatus;
import com.paymentplatform.payment.repository.IdempotencyRecordRepository;
import com.paymentplatform.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyRecordRepository recordRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentStatusCacheService cacheService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private IdempotencyCacheService idempotencyCacheService;

    @InjectMocks
    private PaymentService paymentService;



    @Test
    void createPayment_shouldCreateNewPayment() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest("C101",
                new BigDecimal("5000"), "INR");

        UUID paymentId = UUID.randomUUID();

        when(idempotencyCacheService.get("abc-123"))
                .thenReturn(null);

        when(recordRepository.findByIdempotencyKey("abc-123"))
                .thenReturn(Optional.empty());


        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {

                    Payment payment =
                            invocation.getArgument(0);

                    payment.setId(paymentId);

                    return payment;
                });

        when(objectMapper.writeValueAsString(any(PaymentResponse.class)))
                .thenReturn("{\"paymentId\":\"" + paymentId + "\"}");

        PaymentResponse response =
                paymentService.createPayment(
                        request,
                        "abc-123");

        assertNotNull(response);
        assertEquals(paymentId, response.paymentId());
        assertEquals(new BigDecimal("5000"), response.amount());
        assertEquals("INR", response.currency());
        assertEquals(PaymentStatus.CREATED, response.status());

        verify(paymentRepository).save(any(Payment.class));
        verify(recordRepository).save(any(IdempotencyRecord.class));
        verify(idempotencyCacheService).put(
                eq("abc-123"),
                any(IdempotencyRecord.class));
    }

    @Test
    void createPayment_shouldRejectMissingIdempotencyKey() {
        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        "C101",
                        new BigDecimal("5000"),
                        "INR"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        request,
                        null)
        );

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(recordRepository);
    }

    @Test
    void createPayment_shouldRejectBlankIdempotencyKey() {

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        "C101",
                        new BigDecimal("5000"),
                        "INR"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        request,
                        "   ")
        );

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPayment_shouldReturnExistingPaymentForDuplicateRequest() {

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        "C101",
                        new BigDecimal("5000"),
                        "INR"
                );

        UUID paymentId = UUID.randomUUID();

        IdempotencyRecord record =
                new IdempotencyRecord();

        record.setId(UUID.randomUUID());
        record.setIdempotencyKey("abc-123");
        record.setRequestHash(
                com.paymentplatform.payment.util.RequestHashUtil
                        .generateHash(request));
        record.setPaymentId(paymentId);

        Payment payment = new Payment();

        payment.setId(paymentId);
        payment.setCustomerId("C101");
        payment.setAmount(new BigDecimal("5000"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);

        when(idempotencyCacheService.get("abc-123"))
                .thenReturn(record);

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.createPayment(
                        request,
                        "abc-123");

        assertEquals(paymentId, response.paymentId());
        assertEquals(PaymentStatus.CREATED, response.status());

        verify(paymentRepository)
                .findById(paymentId);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(recordRepository, never())
                .save(any(IdempotencyRecord.class));
    }

}
