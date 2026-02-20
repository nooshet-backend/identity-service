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
import org.nooshet.identity.exception.BadRequestException;
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
        // Normalize inputs and validate separately so we can precisely check existence
        String email = request.getEmail();
        if (email != null) email = email.trim().toLowerCase();
        String mobile = request.getMobile();
        if (mobile != null) mobile = mobile.trim();

        if ((email == null || email.isBlank()) && (mobile == null || mobile.isBlank())) {
            throw new org.nooshet.identity.exception.BadRequestException("Email or mobile must be provided");
        }

        OtpPurpose purpose = request.getPurpose();

        // Rate Limit Check - use whichever identifier is provided for throttling (prefer email)
        String rateLimitIdentifier = (email != null && !email.isBlank()) ? email : mobile;
        var rateLimitResult = otpRateLimiter.checkRateLimit(purpose, rateLimitIdentifier);
        if (!rateLimitResult.isAllowed()) {
            // Log rate limit event for debugging
            System.out.println("[OTP RATE LIMIT] Blocked OTP request for identifier: " + rateLimitIdentifier + ", purpose: " + purpose + ", waitTimeSeconds: " + rateLimitResult.getWaitTimeSeconds());
            throw new OtpRateLimitedException("Rate limit exceeded", rateLimitResult.getWaitTimeSeconds());
        }

        // Check existence separately for email and phone to avoid false positives
        boolean existsByEmail = false;
        boolean existsByPhone = false;
        if (email != null && !email.isBlank()) {
            existsByEmail = userRepository.findByEmail(email).isPresent();
        }
        if (mobile != null && !mobile.isBlank()) {
            existsByPhone = userRepository.findByPhone(mobile).isPresent();
        }
        boolean exists = existsByEmail || existsByPhone;

        // Debug logging to help trace unexpected CONFLICT responses in production
        System.out.println("[OTP] startRegistration check - purpose=" + purpose + ", emailProvided=" + (email != null) + ", emailExists=" + existsByEmail + ", phoneProvided=" + (mobile != null) + ", phoneExists=" + existsByPhone);

        // For registration: we want to block if either email or phone already exist
        if (purpose == OtpPurpose.REGISTRATION && exists) {
            throw new org.nooshet.identity.exception.ConflictException("A user with this email or phone already exists");
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
                .email(email)
                .mobile(mobile)
                // include optional registration data when provided
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .userPasswordHash(request.getUserPasswordHash())
                .build();

        // Choose identifier key for storing OTP session (prefer email)
        String storeIdentifier = (email != null && !email.isBlank()) ? email : mobile;
        otpStore.saveOtpSessionAtomically(purpose, storeIdentifier, sessionId, payload, otpTtlSeconds);

        // Send Email
        if (email != null) {
             Map<String, Object> variables = Map.of(
                 "otp", otp,
                 "expiresIn", otpTtlSeconds / 60
             );
             String templatePath = "src/main/resources/templates/otp-email-template.txt";
             String emailContent = ((org.nooshet.identity.service.impl.EmailServiceImpl) emailService)
                .loadAndFillTemplate(templatePath, variables);
            emailService.sendSimpleEmail(
                email,
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
                .message(OtpMessages.OTP_SENT_IF_EXISTS) // Lie to prevent enumeration
                .build();
    }

    @Override
    public OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        // Since we don't have atomic verify-and-update Lua script in OtpStore yet,
        // we'll implement a basic check. In prod, use Lua.
        
        Optional<OtpSessionPayload> sessionOpt = otpStore.getOtpSession(request.getOtpSessionId());
        if (sessionOpt.isEmpty()) {
            // Missing session likely means expired or invalid session id
            throw new BadRequestException("Invalid or expired OTP session");
        }
        
        OtpSessionPayload session = sessionOpt.get();
        if (Boolean.TRUE.equals(session.getLocked())) {
            // Too many attempts -> inform client it's locked
            throw new BadRequestException(OtpMessages.OTP_LOCKED);
        }
        if (Boolean.TRUE.equals(session.getVerified())) {
            // Session was already used
            throw new BadRequestException(OtpMessages.OTP_ALREADY_VERIFIED);
        }
        
        String inputHash = hashOtp(request.getOtpCode());
        if (!inputHash.equals(session.getOtpHash())) {
            session.setAttempts(session.getAttempts() + 1);
            if (session.getAttempts() >= maxVerifyAttempts) {
                session.setLocked(true);
            }
            otpStore.saveOtpSession(request.getOtpSessionId(), session, otpStore.getExpire(request.getOtpSessionId())); // naive update
            // Incorrect code -> unauthorized
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

        session.setVerified(true);
        otpStore.saveOtpSession(request.getOtpSessionId(), session, otpStore.getExpire(request.getOtpSessionId()));
        
        String identifier = session.getEmail() != null ? session.getEmail() : session.getMobile();
        
        OtpVerifyResponse.OtpVerifyResponseBuilder response = OtpVerifyResponse.builder()
                .verified(true)
                .message(OtpMessages.OTP_VERIFIED);

        if (session.getPurpose() == OtpPurpose.REGISTRATION) {
             // Build JSON payload for registration (include hashed password if present)
             String payloadJson = String.format(
                     "{\"email\":\"%s\",\"mobile\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"userPasswordHash\":\"%s\"}",
                     escapeJson(session.getEmail()),
                     escapeJson(session.getMobile()),
                     escapeJson(session.getFirstName()),
                     escapeJson(session.getLastName()),
                     escapeJson(session.getUserPasswordHash())
             );
             response.registrationToken(registrationTokenService.issueForPayload(payloadJson));
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

    // Simple JSON escaper for null-safe values
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
