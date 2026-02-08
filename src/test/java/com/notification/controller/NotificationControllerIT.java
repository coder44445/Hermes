package com.notification.controller;


import com.notification.event.NotificationEvent;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
    void shouldProcessNotificationSuccessfully() throws Exception {
        NotificationEvent event = new NotificationEvent();
        event.setTenantId("tenant1");
        event.setEventType("PAYMENT_SUCCESS");
        event.setReferenceId("txn_test_1");

        mockMvc.perform(post("/notify")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldProcessNotificationIdempotently() throws Exception {
        NotificationEvent event = new NotificationEvent();
        event.setTenantId("tenant1");
        event.setEventType("PAYMENT_SUCCESS");
        event.setReferenceId("txn_test_1");

        String payload = objectMapper.writeValueAsString(event);

        mockMvc.perform(post("/notify")
                .contentType(APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/notify")
                .contentType(APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());
    }

}
