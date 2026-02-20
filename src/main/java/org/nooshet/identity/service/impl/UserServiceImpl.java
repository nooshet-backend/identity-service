package org.nooshet.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.repository.UserRepository;
import org.nooshet.identity.service.UserService;
import org.springframework.stereotype.Service;
import org.nooshet.identity.entity.Role;
import org.nooshet.identity.dto.CreateProfileRequest;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final org.nooshet.identity.repository.RoleRepository roleRepository;
    private final org.nooshet.identity.client.UserServiceClient userServiceClient;
    private final org.nooshet.identity.service.RegistrationTokenService registrationTokenService;

    @Override
    public User createNewUser(String firstName, String lastName, String passwordHash, String phone, String role, String email) {
        // Validate unique email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new org.nooshet.identity.exception.ConflictException("A user with this email already exists");
        }
        // Validate unique phone
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new org.nooshet.identity.exception.ConflictException("A user with this phone number already exists");
        }

        org.nooshet.identity.entity.Role roleEntity = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Role not found: " + role));

        User user = User.builder()
                .phone(phone)
                .email(email)
                .passwordHash(passwordHash)
                .role(roleEntity)
                .setupRequired(true)
                .build();

        User saved = userRepository.save(user);

        // Create profile in user-service via Feign
        try {
            org.nooshet.identity.dto.CreateProfileRequest profileRequest = org.nooshet.identity.dto.CreateProfileRequest.builder()
                    .userId(saved.getId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phone)
                    .email(email)
                    .role(roleEntity.getName()) // Send role name as String
                    .build();
            
            // TODO: Use a secure internal secret or proper auth
            userServiceClient.createProfile(profileRequest, "change-me-in-prod-please-use-a-longer-secret-key-123456");
        } catch (Exception e) {
            // Log error but allow auth user creation? Or rollback?
            // For now, logging.
            System.err.println("Failed to create profile: " + e.getMessage());
        }

        return saved;
    }

    @Override
    public User completeUserRegistration(String registrationToken, String role) {
        // Use RegistrationTokenService to validate and get the identifier (email)
        String identifier = registrationTokenService.validateAndGetIdentifier(registrationToken);
        if (identifier == null) {
            throw new IllegalArgumentException("Invalid or expired registration token");
        }

        // For demo: fetch registration data from Redis as JSON (assume email as identifier)
        // In a real app, store all registration data (firstName, lastName, passwordHash, phone, email) in Redis at registration start
        // Here, we only have the email, so we will create a minimal user
        if (userRepository.findByEmail(identifier).isPresent()) {
            throw new org.nooshet.identity.exception.ConflictException("User already exists with this email");
        }
        if (userRepository.findByPhone(identifier).isPresent()) {
            throw new org.nooshet.identity.exception.ConflictException("User already exists with this phone number");
        }

        // For demo, use email as both email and phone, and set dummy values for other fields
        String firstName = "User";
        String lastName = "";
        String passwordHash = "";
        String phone = identifier;
        String email = identifier;

        // Find role entity
        org.nooshet.identity.entity.Role roleEntity = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Role not found: " + role));

        User user = User.builder()
                .phone(phone)
                .email(email)
                .passwordHash(passwordHash)
                .role(roleEntity)
                .setupRequired(true)
                .build();

        User saved = userRepository.save(user);
        // Optionally, create profile in user-service
        return saved;
    }

    @Override
    public void markSetupComplete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new org.nooshet.identity.exception.BadRequestException("User not found: " + userId));
        user.setSetupRequired(false);
        userRepository.save(user);
    }
}