package com.notification.event;

import java.util.Map;

import lombok.Data;

@Data
public class NotificationEvent {
    private String tenantId;
    private String eventType;
    private String referenceId;
    private Map<String, Object> payload;

}
