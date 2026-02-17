package org.nooshet.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class OtpRateLimitedException extends BaseException {
    private final long waitTimeSeconds;

    public OtpRateLimitedException(String message, long waitTimeSeconds) {
        super(message);
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }
}
