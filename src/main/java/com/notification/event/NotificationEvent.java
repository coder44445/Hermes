package com.notification.event;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class NotificationEvent {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotBlank(message = "referenceId is required")
    private String referenceId;

    private Map<String, Object> payload;
}
