package com.notification.service;

import com.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSender {

    private final RestTemplate restTemplate;

    /**
     * Send a notification through all available channels.
     * Exceptions in one channel do not prevent others from running.
     */
    public void send(NotificationEvent event) {
        try {
            sendEmail(event);
        } catch (Exception e) {
            log.warn("Email send failed for tenant={}, eventType={}",
                    event.getTenantId(), event.getEventType(), e);
        }

        try {
            sendWebhook(event);
        } catch (Exception e) {
            log.warn("Webhook send failed for tenant={}, eventType={}",
                    event.getTenantId(), event.getEventType(), e);
        }

        try {
            sendInApp(event);
        } catch (Exception e) {
            log.warn("In-app notification failed for tenant={}, eventType={}",
                    event.getTenantId(), event.getEventType(), e);
        }

        log.info("Notification dispatched for tenant={}, eventType={}",
                event.getTenantId(), event.getEventType());
    }

    // ─────────────────────────────────────────────
    // Email channel
    // ─────────────────────────────────────────────
    private void sendEmail(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null || !payload.containsKey("email")) {
            log.debug("No email address in payload — skipping email. tenant={}, eventType={}",
                    event.getTenantId(), event.getEventType());
            return;
        }

        String email   = payload.get("email").toString();
        String subject = "Notification: " + event.getEventType();
        String body    = "Hi, you have a new notification: " + payload;

        // TODO: Replace with JavaMailSender / AWS SES / SendGrid
        log.info("Sending email to {} | subject='{}' | body='{}'", email, subject, body);
    }

    // ─────────────────────────────────────────────
    // Webhook channel
    // ─────────────────────────────────────────────
    private void sendWebhook(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null || !payload.containsKey("webhookUrl")) {
            log.debug("No webhookUrl in payload — skipping webhook. tenant={}, eventType={}",
                    event.getTenantId(), event.getEventType());
            return;
        }

        String url = payload.get("webhookUrl").toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        log.info("Webhook sent to {} | status={}", url, response.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // In-app channel — log only (idempotency record
    // is managed by NotificationProcessor, not here)
    // ─────────────────────────────────────────────
    private void sendInApp(NotificationEvent event) {
        // TODO: Persist to a dedicated in_app_notifications table for front-end polling
        log.debug("In-app notification dispatched for tenant={}, eventType={}",
                event.getTenantId(), event.getEventType());
    }
}
