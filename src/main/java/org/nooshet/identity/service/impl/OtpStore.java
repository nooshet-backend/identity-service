package org.nooshet.identity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nooshet.identity.constants.OtpPurpose;
import org.nooshet.identity.entity.OtpSessionPayload;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OtpStore {

    private final RedisKeyBuilder redisKeyBuilder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveOtpSession(String sessionId, OtpSessionPayload payload, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    redisKeyBuilder.sessionKey(sessionId),
                    json,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OTP session", e);
        }
    }

    public Optional<OtpSessionPayload> getOtpSession(String sessionId) {
        String json = redisTemplate.opsForValue().get(
                redisKeyBuilder.sessionKey(sessionId)
        );

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, OtpSessionPayload.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void saveOtpSessionAtomically(OtpPurpose purpose, String identifier, String sessionId, 
                                        OtpSessionPayload payload, long ttlSeconds) {
        // Simplified version using multi/exec or just separate calls for now
        // Fitnest used Lua script, for simplicity we'll just double write
        // In production, consider Lua for atomicity
        
        saveOtpSession(sessionId, payload, ttlSeconds);
        setActiveSessionPointer(purpose, identifier, sessionId, ttlSeconds);
    }

    public void setActiveSessionPointer(OtpPurpose purpose, String identifier, String sessionId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                redisKeyBuilder.activeSessionKey(purpose, identifier),
                sessionId,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    public Optional<String> getActiveSessionPointer(OtpPurpose purpose, String identifier) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(
                        redisKeyBuilder.activeSessionKey(purpose, identifier)
                )
        );
    }

    public void deleteActivePointer(OtpPurpose purpose, String identifier) {
        redisTemplate.delete(redisKeyBuilder.activeSessionKey(purpose, identifier));
    }

    public void deleteSession(String sessionId) {
        redisTemplate.delete(redisKeyBuilder.sessionKey(sessionId));
    }

    public long getExpire(String sessionId) {
        Long expire = redisTemplate.getExpire(redisKeyBuilder.sessionKey(sessionId), TimeUnit.SECONDS);
        return expire != null ? expire : -1;
    }
}
