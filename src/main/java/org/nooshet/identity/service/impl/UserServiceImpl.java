package org.nooshet.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.repository.UserRepository;
import org.nooshet.identity.service.UserService;
import org.springframework.stereotype.Service;
import org.nooshet.identity.entity.Role;
import org.nooshet.identity.dto.CreateProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.nooshet.identity.exception.BadRequestException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final org.nooshet.identity.repository.RoleRepository roleRepository;
    private final org.nooshet.identity.client.UserServiceClient userServiceClient;
    private final org.nooshet.identity.service.RegistrationTokenService registrationTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // Read payload JSON issued by OtpService
        String payloadJson = registrationTokenService.validateAndGetPayload(registrationToken);
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new BadRequestException("Invalid or expired registration token");
        }

        try {
            Map<String, String> payload = objectMapper.readValue(payloadJson, Map.class);
            String email = payload.getOrDefault("email", "");
            String mobile = payload.getOrDefault("mobile", "");
            String firstName = payload.getOrDefault("firstName", "User");
            String lastName = payload.getOrDefault("lastName", "");
            String passwordHash = payload.getOrDefault("userPasswordHash", "");

            if (email.isBlank() && mobile.isBlank()) {
                throw new BadRequestException("Registration token payload missing identifier");
            }

            String identifier = !email.isBlank() ? email : mobile;

            if (userRepository.findByEmail(email).isPresent()) {
                throw new org.nooshet.identity.exception.ConflictException("User already exists with this email");
            }
            if (userRepository.findByPhone(mobile).isPresent()) {
                throw new org.nooshet.identity.exception.ConflictException("User already exists with this phone number");
            }

            String phone = !mobile.isBlank() ? mobile : identifier;

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
            try {
                org.nooshet.identity.dto.CreateProfileRequest profileRequest = org.nooshet.identity.dto.CreateProfileRequest.builder()
                        .userId(saved.getId())
                        .firstName(firstName)
                        .lastName(lastName)
                        .phoneNumber(phone)
                        .email(email)
                        .role(roleEntity.getName()) // Send role name as String
                        .build();
                userServiceClient.createProfile(profileRequest, "change-me-in-prod-please-use-a-longer-secret-key-123456");
            } catch (Exception e) {
                System.err.println("Failed to create profile: " + e.getMessage());
            }

            return saved;
        } catch (BadRequestException e) {
            // rethrow bad request exceptions as-is
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid registration token payload");
        }
    }

    @Override
    public void markSetupComplete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new org.nooshet.identity.exception.BadRequestException("User not found: " + userId));
        user.setSetupRequired(false);
        userRepository.save(user);
    }
}