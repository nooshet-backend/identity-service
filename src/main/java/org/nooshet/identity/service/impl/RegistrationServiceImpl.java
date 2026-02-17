package org.nooshet.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.nooshet.identity.dto.LoginResponse;
import org.nooshet.identity.dto.RegisterCompleteRequest;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.security.JwtService;
import org.nooshet.identity.service.RegistrationService;
import org.nooshet.identity.service.UserService;
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

    @Override
    @Transactional
    public LoginResponse completeRegistration(RegisterCompleteRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Create user with hashed password
        User user = userService.createNewUser(
                request.getFirstName(),
                request.getLastName(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhoneNumber(),
                request.getUserType(),
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
}
