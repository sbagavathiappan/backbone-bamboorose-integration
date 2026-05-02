package com.backbonebamboorose.repository;

import com.backbonebamboorose.model.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<WebhookEvent> findByStatus(WebhookEvent.EventStatus status);

    Page<WebhookEvent> findByStatus(WebhookEvent.EventStatus status, Pageable pageable);

    List<WebhookEvent> findByQuoteId(String quoteId);

    @Query("SELECT we FROM WebhookEvent we WHERE we.status = 'RETRYING' AND we.nextRetryAt <= :now")
    List<WebhookEvent> findRetriableEvents(@Param("now") OffsetDateTime now);

    @Query("SELECT we FROM WebhookEvent we WHERE we.status = 'PENDING' ORDER BY we.createdAt ASC")
    List<WebhookEvent> findAllPendingEvents();

    @Query("SELECT COUNT(we) FROM WebhookEvent we WHERE we.status = :status")
    long countByStatus(@Param("status") WebhookEvent.EventStatus status);

    @Query("SELECT we FROM WebhookEvent we WHERE we.createdAt BETWEEN :start AND :end ORDER BY we.createdAt DESC")
    Page<WebhookEvent> findByDateRange(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            Pageable pageable);

    List<WebhookEvent> findTop10ByOrderByCreatedAtDesc();
}
