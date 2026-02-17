package org.nooshet.identity.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nooshet.identity.constants.OtpPurpose;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSessionPayload {
    private OtpPurpose purpose;
    private String otpHash;
    private int attempts;
    private Boolean locked;
    private Boolean verified;
    private Instant createdAt;
    
    // Context data
    private String firstName;
    private String lastName;
    private String userPasswordHash;
    private String mobile;
    private String email;
}
