package com.paymentplatform.payment.controller;

import com.paymentplatform.payment.dto.CreatePaymentRequest;
import com.paymentplatform.payment.dto.PaymentResponse;
import com.paymentplatform.payment.dto.UpdatePaymentRequest;
import com.paymentplatform.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
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

}
