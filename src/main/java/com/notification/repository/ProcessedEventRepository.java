package com.notification.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.notification.event.NotificationEvent;
import com.notification.processed.ProcessedEvent;
import com.notification.service.IdempotencyKeyGenerator;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    Optional<ProcessedEvent> findByIdempotencyKey(String idempotencyKey);

    default ProcessedEvent saveProcessed(NotificationEvent event) {
            
        ProcessedEvent processed = new ProcessedEvent();
        
        processed.setIdempotencyKey(IdempotencyKeyGenerator.generate(event));
        processed.setStatus("SUCCESS");
        processed.setProcessedAt(Instant.now());
        processed.setRetryCount(0);

        return save(processed);
    }
}
