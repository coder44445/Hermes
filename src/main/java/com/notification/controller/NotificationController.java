package com.notification.controller;

import com.notification.event.NotificationEvent;
import com.notification.ratelimit.RateLimiterService;
import com.notification.service.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/notify")
public class NotificationController {

    private final NotificationProducer producer;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    public ResponseEntity<String> notify(@RequestBody NotificationEvent event) {

        String tenantId = event.getTenantId();

        // Rate limiting
        if (!rateLimiterService.isAllowed(tenantId)) {
            log.warn("Rate limit exceeded for tenant {}", tenantId);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded for tenant: " + tenantId);
        }

        // Publish to Kafka
        producer.publish(event);
        log.info("Notification event queued in Kafka. tenantId={}, eventType={}", tenantId, event.getEventType());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Notification queued in Kafka");
    }
}
