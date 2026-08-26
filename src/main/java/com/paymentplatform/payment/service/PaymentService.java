package com.paymentplatform.payment.service;

import com.paymentplatform.payment.dto.CreatePaymentRequest;
import com.paymentplatform.payment.dto.PaymentResponse;
import com.paymentplatform.payment.dto.UpdatePaymentRequest;
import com.paymentplatform.payment.entity.Payment;
import com.paymentplatform.payment.entity.PaymentStatus;
import com.paymentplatform.payment.exception.InvalidPaymentStateException;
import com.paymentplatform.payment.exception.PaymentNotFoundException;
import com.paymentplatform.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;


    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }


    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(), payment.getCustomerId(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getCreatedAt()
        );
    }

    @Transactional()
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment(
                //UUID.randomUUID(),
                request.customerId(),
                request.amount(),
                request.currency().toUpperCase(),
                PaymentStatus.CREATED,
                now,
                now
        );

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Saved payment: " + savedPayment.toString());
        return toResponse(savedPayment);

    }

    @Transactional
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

        if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentStateException("Payment cannot be captured from status "
                    + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.AUTHORIZED);
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

    public PaymentStatus getPaymentStatus(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        return payment.getStatus();
    }


}
