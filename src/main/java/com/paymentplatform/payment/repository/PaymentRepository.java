package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.entity.Payment;
import com.paymentplatform.payment.projection.PaymentAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByCustomerId(String customerId);

    @Query("""
        SELECT
            COUNT(p) AS totalPayments,
            COALESCE(SUM(p.amount), 0) AS totalAmount,
            COALESCE(AVG(p.amount), 0) AS averagePaymentAmount
        FROM Payment p
        WHERE p.customerId = :customerId
        """)
    PaymentAggregate getPaymentAggregateByCustomerId(
            @Param("customerId") String customerId
    );

}
