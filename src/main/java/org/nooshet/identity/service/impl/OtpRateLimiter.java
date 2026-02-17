package org.nooshet.identity.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.nooshet.identity.constants.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OtpRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    @Value("${otp.rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${otp.rate-limit.window-minutes}")
    private int windowMinutes;

    @Value("${otp.rate-limit.cooldown-seconds}")
    private int cooldownSeconds;

    public RateLimitResult checkRateLimit(OtpPurpose purpose, String identifier) {
        String key = redisKeyBuilder.rateLimitKey(purpose, identifier);
        
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }

        if (count != null && count > maxAttempts) {
            long ttl = redisTemplate.getExpire(key);
            return new RateLimitResult(false, ttl);
        }

        return new RateLimitResult(true, 0);
    }

    @Getter
    @RequiredArgsConstructor
    public static class RateLimitResult {
        private final boolean allowed;
        private final long waitTimeSeconds;
    }
}
