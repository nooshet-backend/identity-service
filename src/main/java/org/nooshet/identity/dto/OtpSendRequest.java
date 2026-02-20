package org.nooshet.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nooshet.identity.constants.OtpPurpose;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendRequest {
    private String mobile;
    private String email;
    @NotNull
    private OtpPurpose purpose;

    // Optional registration data
    private String firstName;
    private String lastName;
    private String userPasswordHash; // store hashed password, not raw
}
