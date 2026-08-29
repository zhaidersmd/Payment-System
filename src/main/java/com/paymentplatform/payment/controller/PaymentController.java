package com.paymentplatform.payment.controller;

import com.paymentplatform.payment.dto.CreatePaymentRequest;
import com.paymentplatform.payment.dto.PaymentResponse;
import com.paymentplatform.payment.dto.UpdatePaymentRequest;
import com.paymentplatform.payment.entity.PaymentStatus;
import com.paymentplatform.payment.service.PaymentService;
import com.paymentplatform.payment.service.PaymentStatusCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;


    public PaymentController(PaymentService paymentService, PaymentStatusCacheService cacheService) {
        this.paymentService = paymentService;

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a payment",
            description = """
                    Creates a new payment.
                    The Idempotency-Key header is required to prevent
                    duplicate payment creation when clients retry requests.
                    """
    )
    public PaymentResponse createPayment(
            @Parameter(
                    name = "Idempotency-Key",
                    description = "Unique key used to prevent duplicate payment requests",
                    required = true,
                    example = "payment-request-123")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request, idempotencyKey);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {

        return paymentService.getAllPayments();
    }

    @PutMapping("/{paymentId}")
    public PaymentResponse updatePayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentRequest request) {

        return paymentService.updatePayment(
                paymentId,
                request
        );
    }

    @DeleteMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(
            @PathVariable UUID paymentId) {

        paymentService.deletePayment(paymentId);
    }

    @PostMapping("/{id}/authorize")
    public ResponseEntity<PaymentResponse> authorizePayment(
            @PathVariable UUID id) {

        PaymentResponse paymentResponse = paymentService.authorizePayment(id);
        return ResponseEntity.ok(paymentResponse);


    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @PathVariable UUID id) {

        PaymentResponse response = paymentService.capturePayment(id);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID id) {

        PaymentResponse response = paymentService.refundPayment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable UUID paymentId) {

        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));


    }

}
