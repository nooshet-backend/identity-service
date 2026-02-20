package org.nooshet.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "auth:registration:token:";
    private static final Duration TTL = Duration.ofMinutes(30);

    // Store arbitrary JSON payload (e.g., registration data) under a token
    public String issueForPayload(String payloadJson) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, payloadJson, TTL);
        return token;
    }

    // Return the JSON payload and delete the token (one-time use)
    public String validateAndGetPayload(String token) {
        String payload = redisTemplate.opsForValue().get(PREFIX + token);
        if (payload != null) {
             redisTemplate.delete(PREFIX + token); // One-time use
        }
        return payload;
    }
}
