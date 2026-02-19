package org.nooshet.identity.service.impl;

import org.nooshet.identity.constants.OtpMessages;
import org.nooshet.identity.constants.OtpPurpose;
import org.nooshet.identity.dto.OtpSendRequest;
import org.nooshet.identity.dto.OtpSendResponse;
import org.nooshet.identity.dto.OtpVerifyRequest;
import org.nooshet.identity.dto.OtpVerifyResponse;
import org.nooshet.identity.entity.OtpSessionPayload;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.exception.InvalidCredentialsException;
import org.nooshet.identity.exception.OtpRateLimitedException;
import org.nooshet.identity.repository.UserRepository;
import org.nooshet.identity.service.EmailService;
import org.nooshet.identity.service.OtpService;
import org.nooshet.identity.service.RegistrationTokenService;
import org.nooshet.identity.service.ResetPasswordTokenService;
import org.nooshet.identity.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final OtpRateLimiter otpRateLimiter;
    private final OtpGenerator otpGenerator;
    private final EmailService emailService;
    private final RegistrationTokenService registrationTokenService;
    private final ResetPasswordTokenService resetPasswordTokenService;
    
    @Value("${otp.ttl-seconds}")
    private int otpTtlSeconds;

    @Value("${otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${otp.max-verify-attempts}")
    private int maxVerifyAttempts;

    @Override
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        String identifier = request.getEmail();
        if (identifier == null || identifier.isBlank()) {
             // Fallback to mobile if needed, but for now we focus on email
             identifier = request.getMobile();
        }
        
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Email or mobile must be provided");
        }

        OtpPurpose purpose = request.getPurpose();
        
        // Rate Limit Check
        var rateLimitResult = otpRateLimiter.checkRateLimit(purpose, identifier);
        if (!rateLimitResult.isAllowed()) {
            // Log rate limit event for debugging
            System.out.println("[OTP RATE LIMIT] Blocked OTP request for identifier: " + identifier + ", purpose: " + purpose + ", waitTimeSeconds: " + rateLimitResult.getWaitTimeSeconds());
            throw new OtpRateLimitedException("Rate limit exceeded", rateLimitResult.getWaitTimeSeconds());
        }

        boolean exists = userRepository.findByEmail(identifier).isPresent();
        if (!exists && request.getMobile() != null) {
            exists = userRepository.findByPhone(request.getMobile()).isPresent();
        }

        // Security: Don't reveal if user exists or not, but handle logic
        // For registration: we typically want to send OTP only if user DOES NOT exist (or maybe verify email anyway)
        // For password reset: we want to send only if user EXISTS
        if (purpose == OtpPurpose.REGISTRATION && exists) {
            throw new org.nooshet.identity.exception.BadRequestException("A user with this email or phone already exists");
        } else if (purpose == OtpPurpose.PASSWORD_RESET && !exists) {
            throw new org.nooshet.identity.exception.BadRequestException("No user found with this email or phone");
        }

        String otp = otpGenerator.generateOtp();
        String sessionId = java.util.UUID.randomUUID().toString();
        String otpHash = hashOtp(otp);

        OtpSessionPayload payload = OtpSessionPayload.builder()
                .purpose(purpose)
                .otpHash(otpHash)
                .attempts(0)
                .locked(false)
                .verified(false)
                .createdAt(Instant.now())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .build();

        otpStore.saveOtpSessionAtomically(purpose, identifier, sessionId, payload, otpTtlSeconds);

        // Send Email
        if (request.getEmail() != null) {
            Map<String, Object> variables = Map.of(
                "otp", otp,
                "expiresIn", otpTtlSeconds / 60
            );
            String templatePath = "src/main/resources/templates/otp-email-template.txt";
            String emailContent = ((org.nooshet.identity.service.impl.EmailServiceImpl) emailService)
                .loadAndFillTemplate(templatePath, variables);
            emailService.sendSimpleEmail(
                request.getEmail(),
                getSubjectForPurpose(purpose),
                emailContent
            );
        }
        
        return OtpSendResponse.builder()
                .otpSessionId(sessionId)
                .expiresInSeconds(otpTtlSeconds)
                .resendAvailableInSeconds(resendCooldownSeconds)
                .message("OTP sent")
                .build();
    }

    private String getSubjectForPurpose(OtpPurpose purpose) {
        switch (purpose) {
            case REGISTRATION: return "Nooshet Registration Code";
            case PASSWORD_RESET: return "Nooshet Password Reset Code";
            case LOGIN: return "Nooshet Login Code";
            default: return "Nooshet Verification Code";
        }
    }

    private OtpSendResponse createFakeResponse() {
        return OtpSendResponse.builder()
                .otpSessionId(java.util.UUID.randomUUID().toString())
                .expiresInSeconds(otpTtlSeconds)
                .resendAvailableInSeconds(resendCooldownSeconds)
                .message("OTP sent") // Lie to prevent enumeration
                .build();
    }

    @Override
    public OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        // Since we don't have atomic verify-and-update Lua script in OtpStore yet,
        // we'll implement a basic check. In prod, use Lua.
        
        Optional<OtpSessionPayload> sessionOpt = otpStore.getOtpSession(request.getOtpSessionId());
        if (sessionOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid or expired OTP session");
        }
        
        OtpSessionPayload session = sessionOpt.get();
        if (Boolean.TRUE.equals(session.getLocked())) {
            throw new InvalidCredentialsException("OTP session locked");
        }
        if (Boolean.TRUE.equals(session.getVerified())) {
             throw new InvalidCredentialsException("OTP already verified");
        }
        
        String inputHash = hashOtp(request.getOtpCode());
        if (!inputHash.equals(session.getOtpHash())) {
            session.setAttempts(session.getAttempts() + 1);
            if (session.getAttempts() >= maxVerifyAttempts) {
                session.setLocked(true);
            }
            otpStore.saveOtpSession(request.getOtpSessionId(), session, otpStore.getExpire(request.getOtpSessionId())); // naive update
            throw new InvalidCredentialsException("Invalid OTP code");
        }

        session.setVerified(true);
        otpStore.saveOtpSession(request.getOtpSessionId(), session, otpStore.getExpire(request.getOtpSessionId()));
        
        String identifier = session.getEmail() != null ? session.getEmail() : session.getMobile();
        
        OtpVerifyResponse.OtpVerifyResponseBuilder response = OtpVerifyResponse.builder()
                .verified(true)
                .message("Verified");

        if (session.getPurpose() == OtpPurpose.REGISTRATION) {
             response.registrationToken(registrationTokenService.issueForIdentifier(identifier));
        } else if (session.getPurpose() == OtpPurpose.PASSWORD_RESET) {
             response.resetToken(resetPasswordTokenService.issueForIdentifier(identifier));
        }
        
        return response.build();
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
