package org.nooshet.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResponse {
    private String otpSessionId;
    private int expiresInSeconds;
    private int resendAvailableInSeconds;
    private String message;
}
