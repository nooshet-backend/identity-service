package org.nooshet.identity.service.impl;

import lombok.Getter;
import org.nooshet.identity.constants.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {

    @Getter
    @Value("${otp.redis.session-prefix:otp:session:}")
    private String sessionKeyPrefix;

    @Value("${otp.redis.active-session-prefix:otp:active:}")
    private String activeSessionKeyPrefix;

    @Value("${otp.redis.rate-limit-prefix:otp:limit:}")
    private String rateLimitKeyPrefix;

    public String sessionKey(String sessionId) {
        return sessionKeyPrefix + sessionId;
    }

    public String activeSessionKey(OtpPurpose purpose, String identifier) {
        return activeSessionKeyPrefix + purpose.name() + ":" + identifier;
    }

    public String rateLimitKey(OtpPurpose purpose, String identifier) {
        return rateLimitKeyPrefix + purpose.name() + ":" + identifier;
    }
}
