package org.nooshet.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyResponse {
    private boolean verified;
    private String message;
    private String resetToken; // For password reset flow
    private String registrationToken; // For registration flow
}
