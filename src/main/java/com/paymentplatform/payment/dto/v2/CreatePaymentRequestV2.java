package com.paymentplatform.payment.dto.v2;

import com.paymentplatform.payment.entity.PaymentMethod;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class CreatePaymentRequestV2 {
    @NotBlank
    @Size(max = 50)
    String customerId;

    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    String currency;

    PaymentMethod paymentMethod;
}
