package com.paymentplatform.payment.controller;

import com.paymentplatform.payment.dto.PaymentResponse;
import com.paymentplatform.payment.dto.v2.CreatePaymentRequestV2;
import com.paymentplatform.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/payments")
public class PaymentControllerV2 {

    private final PaymentService paymentService;

    public PaymentControllerV2(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequestV2 requestV2,
                                         @RequestHeader("Idempotency-Key") String idempotencyKey){
        return paymentService.createPaymentV2(requestV2, idempotencyKey);


    }
}
