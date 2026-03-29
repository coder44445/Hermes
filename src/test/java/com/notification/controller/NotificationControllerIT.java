package com.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.event.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAcceptNotificationSuccessfully() throws Exception {
        NotificationEvent event = new NotificationEvent();
        event.setTenantId("tenant1");
        event.setEventType("PAYMENT_SUCCESS");
        event.setReferenceId("txn_test_1");

        mockMvc.perform(post("/notify")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted()); // 202
    }

    @Test
    void shouldAcceptDuplicateNotification() throws Exception {
        NotificationEvent event = new NotificationEvent();
        event.setTenantId("tenant1");
        event.setEventType("PAYMENT_SUCCESS");
        event.setReferenceId("txn_test_2");

        String payload = objectMapper.writeValueAsString(event);

        // First call — accepted
        mockMvc.perform(post("/notify")
                .contentType(APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isAccepted());

        // Second call with same event — still accepted at HTTP level (idempotency handled downstream)
        mockMvc.perform(post("/notify")
                .contentType(APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldRejectInvalidEventMissingFields() throws Exception {
        // Missing tenantId, eventType, referenceId — should return 400
        String invalidPayload = "{}";

        mockMvc.perform(post("/notify")
                .contentType(APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
