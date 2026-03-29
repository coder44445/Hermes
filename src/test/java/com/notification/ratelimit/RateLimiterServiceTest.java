package com.notification.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimiterServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RateLimiterService rateLimiterService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = (ValueOperations<String, String>) mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rateLimiterService = new RateLimiterService(redisTemplate);
    }

    @Test
    void shouldAllowRequestsWithinLimit() {
        // Use 1-arg increment() — matches what RateLimiterService actually calls
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L, 3L);

        assertThat(rateLimiterService.isAllowed("tenantA")).isTrue();
        assertThat(rateLimiterService.isAllowed("tenantA")).isTrue();
        assertThat(rateLimiterService.isAllowed("tenantA")).isTrue();
    }

    @Test
    void shouldRejectWhenLimitExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(11L); // default limit = 10

        assertThat(rateLimiterService.isAllowed("tenantA")).isFalse();
    }

    @Test
    void shouldSetExpiryOnFirstRequest() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimiterService.isAllowed("tenantA");

        verify(redisTemplate, times(1)).expire(anyString(), any());
    }

    @Test
    void shouldNotSetExpiryOnSubsequentRequests() {
        when(valueOperations.increment(anyString())).thenReturn(2L); // not the first request

        rateLimiterService.isAllowed("tenantA");

        verify(redisTemplate, never()).expire(anyString(), any());
    }
}
