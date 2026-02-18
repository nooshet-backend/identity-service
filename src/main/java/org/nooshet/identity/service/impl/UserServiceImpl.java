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

    @Override
    public User createNewUser(String firstName, String lastName, String passwordHash, String phone, String role, String email) {
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
            userServiceClient.createProfile(profileRequest, "internal-secret");
        } catch (Exception e) {
            // Log error but allow auth user creation? Or rollback?
            // For now, logging.
            System.err.println("Failed to create profile: " + e.getMessage());
        }

        return saved;
    }
}
