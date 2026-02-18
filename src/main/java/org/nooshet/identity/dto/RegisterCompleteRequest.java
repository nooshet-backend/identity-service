package org.nooshet.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to complete registration after OTP verification")
public class RegisterCompleteRequest {
    @NotBlank(message = "Registration token is required")
    private String registrationToken;
}
