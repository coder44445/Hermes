package com.notification.service;

import com.notification.event.DeadLetterEvent;
import com.notification.event.NotificationEvent;
import com.notification.processed.ProcessedEvent;
import com.notification.repository.DeadLetterEventRepository;
import com.notification.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private static final int MAX_RETRIES = 3;

    private final ProcessedEventRepository repository;

    private final DeadLetterEventRepository deadLetterEventRepository;



    @Transactional
    public void processEvent(NotificationEvent event, Runnable action) {

        String key = IdempotencyKeyGenerator.generate(event);

        if (repository.existsById(key)) {
            return;
        }

        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                action.run();

                repository.save(success(key));
                return;

            } catch (Exception ex) {

                log.warn(
                        "Processing failed. key={}, attempt={}",
                        key,
                        attempt,
                        ex);

                if (attempt >= MAX_RETRIES) {
                    moveToDLQ(event, key, ex, attempt);
                    log.error("Event moved to DLQ. key={}", key);
                    return;
                }
            }
        }
    }

    private void moveToDLQ(
            NotificationEvent event,
            String key,
            Exception ex,
            int retryCount) {

        DeadLetterEvent dlq = new DeadLetterEvent();
        dlq.setEventKey(key);
        dlq.setTenantId(event.getTenantId());
        dlq.setEventType(event.getEventType());
        dlq.setPayload(String.valueOf(event.getPayload()));
        dlq.setFailureReason(
                ex.getMessage() != null
                        ? ex.getMessage().substring(0, Math.min(500, ex.getMessage().length()))
                        : "Unknown error");
        dlq.setRetryCount(retryCount);
        dlq.setLastFailedAt(LocalDateTime.now());

        deadLetterEventRepository.save(dlq);
    }

    private ProcessedEvent success(String key) {
        ProcessedEvent e = new ProcessedEvent();
        e.setIdempotencyKey(key);
        e.setStatus("SUCCESS");
        e.setProcessedAt(Instant.now());
        e.setRetryCount(0);
        return e;
    }
}
