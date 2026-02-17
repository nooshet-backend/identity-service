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

    @Bean
    public CommandLineRunner initRoles() {
        return args -> {
            List<String> roles = Arrays.asList("USER", "COURIER", "CHEF", "ADMIN");
            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    roleRepository.save(new Role(roleName));
                }
            }
        };
    }
}
