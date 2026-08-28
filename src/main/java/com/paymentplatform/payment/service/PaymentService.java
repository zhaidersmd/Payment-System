package com.paymentplatform.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.dto.CreatePaymentRequest;
import com.paymentplatform.payment.dto.PaymentResponse;
import com.paymentplatform.payment.dto.UpdatePaymentRequest;
import com.paymentplatform.payment.entity.IdempotencyRecord;
import com.paymentplatform.payment.entity.Payment;
import com.paymentplatform.payment.entity.PaymentStatus;
import com.paymentplatform.payment.exception.IdempotencyKeyConflictException;
import com.paymentplatform.payment.exception.IdempotencySerializationException;
import com.paymentplatform.payment.exception.InvalidPaymentStateException;
import com.paymentplatform.payment.exception.PaymentNotFoundException;
import com.paymentplatform.payment.repository.IdempotencyRecordRepository;
import com.paymentplatform.payment.repository.PaymentRepository;
import com.paymentplatform.payment.util.RequestHashUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository recordRepository;
    private final ObjectMapper objectMapper;
    private final PaymentStatusCacheService cacheService;
    private final IdempotencyCacheService idempotencyCacheService;
    private final OutboxEventService outboxEventService;


    public PaymentService(PaymentRepository paymentRepository, IdempotencyRecordRepository recordRepository, ObjectMapper objectMapper, PaymentStatusCacheService cacheService, IdempotencyCacheService idempotencyCacheService, OutboxEventService outboxEventService) {
        this.paymentRepository = paymentRepository;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.idempotencyCacheService = idempotencyCacheService;
        this.outboxEventService = outboxEventService;
    }


    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(), payment.getCustomerId(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getCreatedAt()
        );
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {

        LocalDateTime now = LocalDateTime.now();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required");
        }

        String requestHash =
                RequestHashUtil.generateHash(request);
        System.out.println("Hash is " + requestHash);


        // =========================================================
        // CHANGE 1: Check Redis first
        // =========================================================

        IdempotencyRecord existingRecord =
                idempotencyCacheService.get(idempotencyKey);


        // =========================================================
        // CHANGE 2: Redis MISS → check PostgreSQL
        // =========================================================

        if (existingRecord == null) {
            System.out.println("existingRecord == null");
            existingRecord =
                    recordRepository
                            .findByIdempotencyKey(idempotencyKey)
                            .orElse(null);


            // =====================================================
            // CHANGE 3: Found in PostgreSQL → populate Redis
            // =====================================================

            if (existingRecord != null) {
                System.out.println("Found in PostgreSQL → populate Redis");
                idempotencyCacheService.put(
                        idempotencyKey,
                        existingRecord);
            }
        }


        // =========================================================
        // Existing idempotency record
        // =========================================================

        if (existingRecord != null) {
            System.out.println("Existing idempotency record. Sending from Redis");

            if (!existingRecord.getRequestHash()
                    .equals(requestHash)) {

                throw new IdempotencyKeyConflictException(
                        "Idempotency-Key has already been used with a different request");
            }

            IdempotencyRecord finalExistingRecord = existingRecord;
            Payment payment =
                    paymentRepository.findById(
                                    existingRecord.getPaymentId())
                            .orElseThrow(() ->
                                    new PaymentNotFoundException(
                                            finalExistingRecord.getPaymentId()));

            return toResponse(payment);
        }


        // =========================================================
        // New request → create payment
        // =========================================================

        Payment payment = new Payment();

        System.out.println("Creating payment object--");
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        Payment savedPayment = paymentRepository.save(payment);


//        Tested if outbox event does not save, payment instance will be rolled back
//        if (1 == 1) {
//            throw new RuntimeException("TEST OUTBOX FAILURE");
//        }



        outboxEventService.createPaymentCreatedEvent(
                savedPayment.getId(),
                savedPayment.getCustomerId(),
                savedPayment.getAmount(),
                savedPayment.getCurrency()
        );



        PaymentResponse response =
                toResponse(savedPayment);


        // =========================================================
        // Create idempotency record
        // =========================================================
        System.out.println("Creating idempotency object--");
        IdempotencyRecord record =
                new IdempotencyRecord();

        record.setId(UUID.randomUUID());
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setPaymentId(savedPayment.getId());
        record.setResponseStatus(201);
        record.setCreatedAt(now);

        try {
            System.out.println("Writing Response");
            String responseBody =
                    objectMapper.writeValueAsString(response);

            record.setResponseBody(responseBody);

        } catch (JsonProcessingException e) {

            throw new IdempotencySerializationException(
                    "Failed to serialize payment response",
                    e);
        }


        // =========================================================
        // PostgreSQL remains the source of truth
        // =========================================================

        recordRepository.save(record);


        // =========================================================
        // CHANGE 4: Populate Redis after DB save
        // =========================================================

        idempotencyCacheService.put(
                idempotencyKey,
                record);


        return response;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow( () -> new PaymentNotFoundException(paymentId));
        log.info("Found the payment instance {}" , payment);
        return toResponse(payment);
    }

    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse updatePayment(
            UUID paymentId,
            UpdatePaymentRequest request) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setCurrency(
                request.currency().toUpperCase()
        );

        Payment updatedPayment =
                paymentRepository.save(payment);

        return toResponse(updatedPayment);
    }

    public void deletePayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        paymentRepository.delete(payment);
    }

    @Transactional
    public PaymentResponse authorizePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new InvalidPaymentStateException(
                    "Payment cannot be authorized from status " + payment.getStatus());
        }
        payment.setStatus(PaymentStatus.AUTHORIZED);
        return toResponse(payment);

    }
    @Transactional
    public PaymentResponse capturePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentStateException("Payment cannot be captured from status "
                    + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CAPTURED);
        return toResponse(payment);
    }
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId ));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new InvalidPaymentStateException(
                    "Payment cannot be refunded from status "
                            + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.REFUNDED);

        return toResponse(payment);
    }


    public PaymentResponse getPaymentStatus(UUID paymentId) {
        log.info("going to check if cache exists");
        PaymentResponse cachedResponse = cacheService.get(paymentId);

        if (cachedResponse != null) {
            log.info("Sending data from redis");
            return cachedResponse;
        }
        log.info("Sending data from postgres");
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        PaymentResponse response = toResponse(payment);

        cacheService.put(paymentId, response);

        return response;
    }


}
