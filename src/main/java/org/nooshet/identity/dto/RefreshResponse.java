package org.nooshet.identity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshResponse {
    private String accessToken;
    private String refreshToken;
}

