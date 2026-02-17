package org.nooshet.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "Request to initiate registration by collecting user data and sending OTP")
public class RegisterRequest {

    @NotBlank(message = "Phone number is required")
    @Schema(description = "User's phone number", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;
}

