package org.nooshet.identity.config;

import lombok.RequiredArgsConstructor;
import org.nooshet.identity.entity.Role;
import org.nooshet.identity.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final org.nooshet.identity.repository.UserRepository userRepository;
    private final org.nooshet.identity.service.PasswordService passwordService;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Init Roles
            List<String> roles = Arrays.asList("USER", "COURIER", "CHEF", "ADMIN");
            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    roleRepository.save(new Role(roleName));
                }
            }

            // Init Admin User
            String adminEmail = "admin@nooshet.com";
            String adminPhone = "+1234567890";
            if (userRepository.findByEmail(adminEmail).isEmpty() && userRepository.findByPhone(adminPhone).isEmpty()) {
                Role adminRole = roleRepository.findByName("ADMIN")
                        .orElseThrow(() -> new RuntimeException("ADMIN role not found after initialization"));

                org.nooshet.identity.entity.User admin = org.nooshet.identity.entity.User.builder()
                        .email(adminEmail)
                        .phone(adminPhone)
                        .passwordHash(passwordService.hashPassword("admin_password"))
                        .role(adminRole)
                        .setupRequired(false)
                        .build();

                userRepository.save(admin);
                System.out.println("Initial admin user created: " + adminEmail);
            }
        };
    }
}
