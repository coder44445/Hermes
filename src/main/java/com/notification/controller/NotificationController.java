package com.notification.controller;

import com.notification.event.NotificationEvent;
import com.notification.ratelimit.RateLimiterService;
import com.notification.service.NotificationProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
public class NotificationController {

    private final NotificationProcessor processor;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/notify")
    public ResponseEntity<String> notify(@RequestBody NotificationEvent event) {

        String tenantId = event.getTenantId();

        if (!rateLimiterService.isAllowed(tenantId)) {
            log.warn("Rate limit exceeded for tenant {}", tenantId);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded for tenant: " + tenantId);
        }

        processor.processEvent(event, () -> {
            log.info(
                    "Processing notification. tenantId={}, eventType={}",
                    event.getTenantId(),
                    event.getEventType());
        });

        return ResponseEntity.ok("Notification queued");
    }
}
