package org.nooshet.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.nooshet.identity.constants.OtpPurpose;
import org.nooshet.identity.dto.OtpSendRequest;
import org.nooshet.identity.dto.OtpSendResponse;
import org.nooshet.identity.dto.OtpVerifyRequest;
import org.nooshet.identity.dto.OtpVerifyResponse;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.exception.BadRequestException;
import org.nooshet.identity.exception.InvalidCredentialsException;
import org.nooshet.identity.repository.UserRepository;
import org.nooshet.identity.service.OtpService;
import org.nooshet.identity.service.PasswordResetService;
import org.nooshet.identity.service.ResetPasswordTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final OtpService otpService;
    private final ResetPasswordTokenService resetPasswordTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OtpSendResponse requestPasswordReset(String identifier) {
        // Determine if identifier is email or phone
        String email = identifier.contains("@") ? identifier : null;
        String mobile = email == null ? identifier : null;

        return otpService.sendOtp(OtpSendRequest.builder()
                .email(email)
                .mobile(mobile)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .build());
    }

    @Override
    public OtpVerifyResponse verifyOtp(String sessionId, String code) {
        return otpService.verifyOtpAndIssueToken(OtpVerifyRequest.builder()
                .otpSessionId(sessionId)
                .otpCode(code)
                .build());
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String identifier = resetPasswordTokenService.validateAndGetIdentifier(resetToken);
        if (identifier == null) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = null;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier).orElse(null);
        } else {
            user = userRepository.findByPhone(identifier).orElse(null);
        }

        if (user == null) {
            // Should not happen if token was issued for existing user, but handle it
            throw new InvalidCredentialsException("User not found");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
