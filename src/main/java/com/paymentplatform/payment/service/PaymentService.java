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


    public PaymentService(PaymentRepository paymentRepository, IdempotencyRecordRepository recordRepository, ObjectMapper objectMapper, PaymentStatusCacheService cacheService) {
        this.paymentRepository = paymentRepository;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
    }


    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(), payment.getCustomerId(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getCreatedAt()
        );
    }

    @Transactional()
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        log.info("CreatePaymentRequest = {}", request);

        LocalDateTime now = LocalDateTime.now();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required");
        }

        String requestHash =
                RequestHashUtil.generateHash(request);
        log.info("Request hash is {}", requestHash);

        Optional<IdempotencyRecord> existingRecord =
                recordRepository.findByIdempotencyKey(idempotencyKey);

        log.info("Recording is existing or not {}", existingRecord.orElseGet(() -> null));

        if (existingRecord.isPresent()) {
            log.info("Record is already present: {}", existingRecord);

            IdempotencyRecord record = existingRecord.get();
            // handle existing request
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyConflictException(
                        "Idempotency-Key has already been used with a different request");
            }
            // if hash did not match
            else {
                log.info("Hash did not match");
                Payment payment =
                        paymentRepository.findById(record.getPaymentId())
                                .orElseThrow(() ->
                                        new PaymentNotFoundException(
                                                record.getPaymentId()));
                return toResponse(payment);

            }
        }


        log.info("Record is not present: {}", existingRecord);
        Payment payment = new Payment();


        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        log.info("Saving payment record");
        Payment savedPayment =
                paymentRepository.save(payment);

        PaymentResponse response = toResponse(savedPayment);

        // -----------------------------
        // Create idempotency record
        // -----------------------------
        log.info("Saving idempotency record");
        IdempotencyRecord record = new IdempotencyRecord();

        record.setId(UUID.randomUUID());
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setPaymentId(savedPayment.getId());
        record.setResponseStatus(201);
        record.setCreatedAt(now);



        try {
            String responseBody =
                    objectMapper.writeValueAsString(response);
                    record.setResponseBody(responseBody);
        } catch (JsonProcessingException e) {
            throw new IdempotencySerializationException(
                    "Failed to serialize payment response", e);
        }
        recordRepository.save(record);

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
