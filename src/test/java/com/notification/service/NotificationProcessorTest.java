package com.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.event.NotificationEvent;
import com.notification.processed.ProcessedEvent;
import com.notification.repository.DeadLetterEventRepository;
import com.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.util.Map;

class NotificationProcessorTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private DeadLetterEventRepository deadLetterEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProcessEventSuccessfully() {
        String key = "tenant1:NORMAL:ref-123";

        NotificationEvent event = new NotificationEvent();
        event.setTenantId("tenant1");
        event.setEventType("NORMAL");
        event.setReferenceId("ref-123");
        event.setPayload(Map.of("message", "Hello World"));

        when(processedEventRepository.existsById(key)).thenReturn(false);

        Runnable action = mock(Runnable.class);

        processor.processEvent(event, action);

        verify(action, times(1)).run();
        verify(processedEventRepository, times(1)).save(argThat(e ->
                e.getStatus().equals("SUCCESS") && e.getIdempotencyKey().equals(key)
        ));
        verify(deadLetterEventRepository, never()).save(any());
    }

    @Test
    void shouldSkipDuplicateEvent() {
        String key = "tenant1:NORMAL:ref-123";

        NotificationEvent event = new NotificationEvent();
        event.setTenantId("tenant1");
        event.setEventType("NORMAL");
        event.setReferenceId("ref-123");
        event.setPayload(Map.of("message", "Hello World"));

        when(processedEventRepository.existsById(key)).thenReturn(true);

        Runnable action = mock(Runnable.class);
        processor.processEvent(event, action);

        verify(action, never()).run();
        verify(processedEventRepository, never()).save(any());
        verify(deadLetterEventRepository, never()).save(any());
    }

    @Test
    void shouldMoveEventToDLQAfterMaxRetries() throws Exception {
        NotificationEvent event = new NotificationEvent();
        event.setTenantId("tenant1");
        event.setEventType("FAIL");
        event.setReferenceId("ref-123");
        event.setPayload(Map.of("message", "Test failure"));

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"message\":\"Test failure\"}");

        Runnable failingAction = () -> {
            throw new RuntimeException("Simulated failure");
        };

        processor.processEvent(event, failingAction);

        // Should save a FAILED ProcessedEvent to prevent re-processing
        verify(processedEventRepository, times(1)).save(argThat(e ->
                e.getStatus().equals("FAILED") && e.getRetryCount() == 3
        ));

        // Should move to DLQ
        verify(deadLetterEventRepository, times(1)).save(argThat(dlq ->
                dlq.getTenantId().equals("tenant1") &&
                dlq.getRetryCount() == 3 &&
                dlq.getEventKey().equals(IdempotencyKeyGenerator.generate(event))
        ));
    }
}
