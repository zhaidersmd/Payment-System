package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.entity.OutboxEvent;
import com.paymentplatform.payment.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus outboxEventStatus);
}
