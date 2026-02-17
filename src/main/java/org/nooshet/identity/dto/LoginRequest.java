package org.nooshet.identity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Email
    private String email;
    
    @jakarta.validation.constraints.NotBlank
    private String password;
}

