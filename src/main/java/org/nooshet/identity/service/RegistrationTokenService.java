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

    public String issueForIdentifier(String identifier) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, identifier, TTL);
        return token;
    }

    public String validateAndGetIdentifier(String token) {
        String identifier = redisTemplate.opsForValue().get(PREFIX + token);
        if (identifier != null) {
             redisTemplate.delete(PREFIX + token); // One-time use
        }
        return identifier;
    }
}
