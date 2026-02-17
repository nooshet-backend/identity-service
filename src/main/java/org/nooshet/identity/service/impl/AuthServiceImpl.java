package org.nooshet.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.nooshet.identity.dto.LoginRequest;
import org.nooshet.identity.dto.LoginResponse;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.repository.UserRepository;
import org.nooshet.identity.security.JwtService;
import org.nooshet.identity.service.AuthService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. Find User
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
             throw new org.nooshet.identity.exception.InvalidCredentialsException("Invalid username or password");
        }

        // 2. Verify Password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new org.nooshet.identity.exception.InvalidCredentialsException("Invalid username or password");
        }

        // 3. Generate Tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), java.util.Collections.singletonList(user.getRole().getName()));
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 4. Return Response
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L) // Should get from properties
                .build();
    }
}
