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

    @Operation(summary = "Start registration: send OTP to courier")
    @PostMapping("/register/courier")
    public ResponseEntity<OtpSendResponse> startCourierRegistration(@Valid @RequestBody RegisterRequest request) {
        OtpSendResponse response = registrationService.startRegistration(request, "COURIER");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Start registration: send OTP to user")
    @PostMapping("/register/user")
    public ResponseEntity<OtpSendResponse> startUserRegistration(@Valid @RequestBody RegisterRequest request) {
        OtpSendResponse response = registrationService.startRegistration(request, "USER");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Start registration: send OTP to chef")
    @PostMapping("/register/chef")
    public ResponseEntity<OtpSendResponse> startChefRegistration(@Valid @RequestBody RegisterRequest request) {
        OtpSendResponse response = registrationService.startRegistration(request, "CHEF");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Complete registration for courier after OTP verification")
    @PostMapping("/register/courier/complete")
    public ResponseEntity<LoginResponse> completeCourierRegistration(@Valid @RequestBody RegisterCompleteRequest request) {
        LoginResponse response = registrationService.completeRegistration(request, "COURIER");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Complete registration for user after OTP verification")
    @PostMapping("/register/user/complete")
    public ResponseEntity<LoginResponse> completeUserRegistration(@Valid @RequestBody RegisterCompleteRequest request) {
        LoginResponse response = registrationService.completeRegistration(request, "USER");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Complete registration for chef after OTP verification")
    @PostMapping("/register/chef/complete")
    public ResponseEntity<LoginResponse> completeChefRegistration(@Valid @RequestBody RegisterCompleteRequest request) {
        LoginResponse response = registrationService.completeRegistration(request, "CHEF");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Verify registration OTP")
    @PostMapping("/register/verify-otp")
    public ResponseEntity<OtpVerifyResponse> verifyRegistrationOtp(@Valid @RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = registrationService.verifyRegistrationOtp(request);
        return ResponseEntity.ok(response);
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

    // --- HOME COOK REGISTRATION ---
    @Operation(summary = "Register Home Cook (step 1: basic info + FIN code)")
    @PostMapping("/register/cook")
    public ResponseEntity<Void> registerCook(@Valid @RequestBody CookRegistrationRequest request) {
        // TODO: Implement registration logic
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Upload Home Cook kitchen photos")
    @PutMapping("/register/cook/kitchen-photos")
    public ResponseEntity<Void> uploadCookKitchenPhotos(@RequestPart CookKitchenPhotoRequest request) {
        // TODO: Save kitchen photos, show warning about future AI/moderator checks
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete Home Cook kitchen photo")
    @DeleteMapping("/register/cook/kitchen-photos")
    public ResponseEntity<Void> deleteCookKitchenPhoto(@RequestParam String photoId) {
        // TODO: Delete kitchen photo by ID
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Home Cook rules acceptance (no skip)")
    @PostMapping("/register/cook/rules")
    public ResponseEntity<Void> acceptCookRules(@Valid @RequestBody CookRulesAcceptanceRequest request) {
        // TODO: Enforce scroll/read to end before accepting
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Admin approval for Home Cook (internal)")
    @PostMapping("/register/cook/verify")
    public ResponseEntity<Void> verifyCook(@RequestParam String cookId) {
        // TODO: Admin approval logic
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get Home Cook badge status")
    @GetMapping("/register/cook/badge")
    public ResponseEntity<String> getCookBadgeStatus(@RequestParam String cookId) {
        // TODO: Return badge status
        return ResponseEntity.ok("PENDING");
    }

    // --- COURIER REGISTRATION ---
    @Operation(summary = "Register Courier (basic info, FIN code, transport)")
    @PostMapping("/register/courier/setup")
    public ResponseEntity<Void> registerCourier(@Valid @RequestBody CourierRegistrationRequest request) {
        // TODO: Implement registration logic
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Upload Courier self-photo (one-time only)")
    @PutMapping("/register/courier/selfie")
    public ResponseEntity<Void> uploadCourierSelfie(/*@RequestPart CourierSelfieRequest request*/) {
        // TODO: Enforce one-time upload, save selfie
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete Courier self-photo")
    @DeleteMapping("/register/courier/selfie")
    public ResponseEntity<Void> deleteCourierSelfie(@RequestParam String courierId) {
        // TODO: Delete courier selfie by courier ID
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Courier rules acceptance (no skip)")
    @PostMapping("/register/courier/rules")
    public ResponseEntity<Void> acceptCourierRules(/*@Valid @RequestBody CourierRulesAcceptanceRequest request*/) {
        // TODO: Enforce scroll/read to end before accepting
        return ResponseEntity.ok().build();
    }

    // --- BUYER REGISTRATION ---
    @Operation(summary = "Register Buyer (basic info + address)")
    @PostMapping("/register/buyer")
    public ResponseEntity<Void> registerBuyer(@Valid @RequestBody BuyerRegistrationRequest request) {
        // TODO: Implement registration logic
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Buyer rules acceptance (no skip)")
    @PostMapping("/register/buyer/rules")
    public ResponseEntity<Void> acceptBuyerRules(@Valid @RequestBody BuyerRulesAcceptanceRequest request) {
        // TODO: Enforce scroll/read to end before accepting
        return ResponseEntity.ok().build();
    }
}
