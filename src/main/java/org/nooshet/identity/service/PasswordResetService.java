package org.nooshet.identity.service;

import org.nooshet.identity.dto.OtpSendResponse;
import org.nooshet.identity.dto.OtpVerifyResponse;

public interface PasswordResetService {
    OtpSendResponse requestPasswordReset(String identifier);
    OtpVerifyResponse verifyOtp(String sessionId, String code);
    void resetPassword(String resetToken, String newPassword);
}
