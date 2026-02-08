package com.notification.service;

import com.notification.event.NotificationEvent;

public class IdempotencyKeyGenerator {
    public static String generate(NotificationEvent event) {
        return event.getTenantId() + ":" + event.getEventType() + ":" + event.getReferenceId();
    }
}