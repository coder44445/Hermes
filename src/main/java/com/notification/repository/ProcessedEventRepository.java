package com.notification.repository;

import com.notification.processed.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for idempotency tracking.
 * Business logic for building ProcessedEvent objects lives in NotificationProcessor.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    Optional<ProcessedEvent> findByIdempotencyKey(String idempotencyKey);
}
