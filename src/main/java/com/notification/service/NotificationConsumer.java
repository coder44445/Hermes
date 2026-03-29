package com.notification.service;

import com.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationProcessor processor;
    private final NotificationSender sender;

    @KafkaListener(topics = "${notification.kafka.topic:notification-events}", groupId = "notification-group")
    public void consume(NotificationEvent event) {
        processor.processEvent(event, () -> {
            sender.send(event);
            log.info("Processed notification from Kafka. tenantId={}, eventType={}", event.getTenantId(),
                    event.getEventType());
        });
    }
}
