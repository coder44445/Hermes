package com.notification.service;

import com.notification.event.NotificationEvent;
import com.notification.repository.ProcessedEventRepository;
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

    private final ProcessedEventRepository processedEventRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send a notification through all available channels.
     * Exceptions in one channel do not prevent others.
     */
    public void send(NotificationEvent event) {
        try {
            sendEmail(event);
        } catch (Exception e) {
            log.warn("Email send failed for tenant={}, eventType={}", event.getTenantId(), event.getEventType(), e);
        }

        try {
            sendWebhook(event);
        } catch (Exception e) {
            log.warn("Webhook send failed for tenant={}, eventType={}", event.getTenantId(), event.getEventType(), e);
        }

        try {
            sendInApp(event);
        } catch (Exception e) {
            log.warn("In-app notification failed for tenant={}, eventType={}", event.getTenantId(),
                    event.getEventType(), e);
        }

        log.info("Notification processed for tenant={}, eventType={}", event.getTenantId(), event.getEventType());
    }

    // -----------------------------
    // Email channel (mock / real)
    // -----------------------------
    private void sendEmail(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null || !payload.containsKey("email")) {
            log.debug("No email address provided for tenant={}, eventType={}", event.getTenantId(),
                    event.getEventType());
            return;
        }

        String email = payload.get("email").toString();
        String subject = "Notification: " + event.getEventType();
        String body = "Hi, you have a new notification: " + payload;

        // TODO: Replace with JavaMailSender if sending real emails
        log.info("Sending email to {} | subject='{}' | body='{}'", email, subject,body);
    }

    // -----------------------------
    // Webhook channel
    // -----------------------------
    private void sendWebhook(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null || !payload.containsKey("webhookUrl")) {
            return;
        }

        String url = payload.get("webhookUrl").toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        log.info("Webhook sent to {} | status={}", url, response.getStatusCode());
    }

    // -----------------------------
    // In-app notifications
    // -----------------------------
    private void sendInApp(NotificationEvent event) {
        processedEventRepository.saveProcessed(event);
        log.debug("In-app notification saved for tenant={}, eventType={}", event.getTenantId(), event.getEventType());
    }
}
