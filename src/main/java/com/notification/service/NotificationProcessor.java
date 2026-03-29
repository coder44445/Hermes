package com.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ProcessedEventRepository processedEventRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processEvent(NotificationEvent event, Runnable action) {
        String key = IdempotencyKeyGenerator.generate(event);

        // Idempotency check — skip if already processed (success or failure)
        if (processedEventRepository.existsById(key)) {
            log.info("Duplicate event detected, skipping. key={}", key);
            return;
        }

        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                action.run();

                processedEventRepository.save(buildProcessedEvent(key, "SUCCESS", attempt - 1, null));
                log.info("Event processed successfully. key={}, attempt={}", key, attempt);
                return;

            } catch (Exception ex) {
                log.warn("Processing failed. key={}, attempt={}/{}", key, attempt, MAX_RETRIES, ex);

                if (attempt >= MAX_RETRIES) {
                    // Save a FAILED record so this event is not retried again on re-delivery
                    processedEventRepository.save(buildProcessedEvent(key, "FAILED", attempt, ex.getMessage()));
                    moveToDLQ(event, key, ex, attempt);
                    log.error("Event moved to DLQ after {} attempts. key={}", attempt, key);
                    return;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────

    private ProcessedEvent buildProcessedEvent(String key, String status, int retryCount, String errorMessage) {
        ProcessedEvent event = new ProcessedEvent();
        event.setIdempotencyKey(key);
        event.setStatus(status);
        event.setProcessedAt(Instant.now());
        event.setRetryCount(retryCount);
        event.setErrorMessage(errorMessage);
        return event;
    }

    private void moveToDLQ(NotificationEvent event, String key, Exception ex, int retryCount) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(event.getPayload());
        } catch (JsonProcessingException e) {
            payloadJson = "{}";
            log.warn("Could not serialize payload for DLQ. key={}", key, e);
        }

        String failureReason = ex.getMessage() != null
                ? ex.getMessage().substring(0, Math.min(500, ex.getMessage().length()))
                : "Unknown error";

        DeadLetterEvent dlq = new DeadLetterEvent();
        dlq.setEventKey(key);
        dlq.setTenantId(event.getTenantId());
        dlq.setEventType(event.getEventType());
        dlq.setPayload(payloadJson);
        dlq.setFailureReason(failureReason);
        dlq.setRetryCount(retryCount);
        dlq.setLastFailedAt(LocalDateTime.now());

        deadLetterEventRepository.save(dlq);
    }
}
