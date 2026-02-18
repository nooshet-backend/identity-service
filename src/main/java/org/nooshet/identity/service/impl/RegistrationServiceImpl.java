package org.nooshet.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.nooshet.identity.dto.LoginResponse;
import org.nooshet.identity.dto.OtpSendResponse;
import org.nooshet.identity.dto.OtpVerifyRequest;
import org.nooshet.identity.dto.OtpVerifyResponse;
import org.nooshet.identity.dto.RegisterCompleteRequest;
import org.nooshet.identity.dto.RegisterRequest;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.security.JwtService;
import org.nooshet.identity.service.RegistrationService;
import org.nooshet.identity.service.UserService;
import org.nooshet.identity.service.OtpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Override
    @Transactional
    public LoginResponse completeRegistration(RegisterCompleteRequest request, String role) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Create user with hashed password and provided role
        User user = userService.createNewUser(
                request.getFirstName(),
                request.getLastName(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhoneNumber(),
                role,
                request.getEmail()
        );

        // Generate Tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), Collections.singletonList(user.getRole().getName()));
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L) // Should utilize properties
                .build();
    }

    @Override
    public OtpSendResponse startRegistration(RegisterRequest request, String role) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        // Use OtpPurpose.REGISTRATION, send OTP to email
        return otpService.sendOtp(
            org.nooshet.identity.dto.OtpSendRequest.builder()
                .email(request.getEmail())
                .purpose(org.nooshet.identity.constants.OtpPurpose.REGISTRATION)
                .build()
        );
    }

    @Override
    public OtpVerifyResponse verifyRegistrationOtp(OtpVerifyRequest request) {
        return otpService.verifyOtpAndIssueToken(request);
    }
}
