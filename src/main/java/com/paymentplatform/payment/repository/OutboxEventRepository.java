package com.paymentplatform.payment.repository;

import com.paymentplatform.payment.entity.OutboxEvent;
import com.paymentplatform.payment.entity.OutboxEventStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus outboxEventStatus);

    @Modifying
    @Transactional
    @Query("""
        UPDATE OutboxEvent e
        SET e.status = :processingStatus, e.processingStartedAt = :processingStartedAt
        WHERE e.id = :eventId
          AND e.status = :pendingStatus
        """)
    int claimEvent(
            @Param("eventId") UUID eventId,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("processingStartedAt") LocalDateTime processingStartedAt);


    @Modifying
    @Query("""
        UPDATE OutboxEvent e
        SET e.status = :pendingStatus,
            e.processingStartedAt = NULL
        WHERE e.status = :processingStatus
          AND e.processingStartedAt < :cutoff
        """)
    int recoverStuckEvents(
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("cutoff") LocalDateTime cutoff);
}
