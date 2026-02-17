package org.nooshet.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nooshet.identity.dto.*;
import org.nooshet.identity.service.AuthService;
import org.nooshet.identity.service.PasswordResetService;
import org.nooshet.identity.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication, token management, and registration")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "User login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register (collects name, phone, email, password twice and creates user)")
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterCompleteRequest request) {
        LoginResponse response = registrationService.completeRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Password Reset Endpoints

    @Operation(summary = "Request password reset OTP via email or SMS")
    @PostMapping("/password-reset/request")
    public ResponseEntity<OtpSendResponse> requestPasswordReset(@RequestParam String identifier) {
        OtpSendResponse response = passwordResetService.requestPasswordReset(identifier);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify password reset OTP")
    @PostMapping("/password-reset/verify")
    public ResponseEntity<OtpVerifyResponse> verifyPasswordResetOtp(@RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = passwordResetService.verifyOtp(request.getOtpSessionId(), request.getOtpCode());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Complete password reset with new password")
    @PostMapping("/password-reset/complete")
    public ResponseEntity<Void> completePasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
