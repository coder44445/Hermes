package com.notification.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${ratelimit.per-minute:10}")
    private int limit;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String tenantId) {
        long currentMinute = Instant.now()
                .truncatedTo(ChronoUnit.MINUTES)
                .getEpochSecond();

        String key = String.format("rate:%s:%d", tenantId, currentMinute);

        Long current = redisTemplate.opsForValue().increment(key);

        if (current != null && current == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        return current != null && current <= limit;
    }
}
