package org.nooshet.identity.dto;

import lombok.Builder;
import lombok.Data;
import org.nooshet.identity.entity.Role;

@Data
@Builder
public class CreateProfileRequest {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
}
