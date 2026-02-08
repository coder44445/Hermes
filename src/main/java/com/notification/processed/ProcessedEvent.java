package com.notification.processed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Data
public class ProcessedEvent {

    @Id
    private String idempotencyKey;

    private Instant processedAt;

    private String status;

    private int retryCount;
    private String errorMessage;

    public ProcessedEvent() {}

    public ProcessedEvent(String idempotencyKey, Instant processedAt, String status) {
        this.idempotencyKey = idempotencyKey;
        this.processedAt = processedAt;
        this.status = status;
    }

}
