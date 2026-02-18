package org.nooshet.identity.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Value("${springdoc.server-url:}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Identity Service API")
                        .version("1.0.0")
                        .description("Identity and Access Management Service - Handles authentication, registration, OTP verification, and user management")
                        .contact(new Contact().name("Nooshet Team").email("support@nooshet.local")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        if (serverUrl != null && !serverUrl.isEmpty()) {
            openAPI.setServers(java.util.List.of(new io.swagger.v3.oas.models.servers.Server().url(serverUrl)));
        }
        return openAPI;
    }

    @Bean
    public org.springdoc.core.customizers.OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getDescription() != null && operation.getDescription().isEmpty()) {
                operation.setDescription(null);
            }
            return operation;
        };
    }
}

