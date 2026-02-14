package com.notification.service;

import com.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${notification.kafka.topic:notification-events}")
    private String topic;

    /**
     * Publish a notification event to Kafka asynchronously using CompletableFuture.
     */
    public void publish(NotificationEvent event) {
        if (event == null || event.getTenantId() == null) {
            log.warn("Cannot publish null event or missing tenantId");
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return kafkaTemplate.send(topic, event.getTenantId(), event).get();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event to Kafka topic '{}' for tenant={}", topic, event.getTenantId(),
                                ex);
                    } else {
                        log.info("Event sent to Kafka topic '{}' for tenant={}", topic, event.getTenantId());
                    }
                });
    }
}
