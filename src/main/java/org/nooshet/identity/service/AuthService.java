package org.nooshet.identity.service;

import org.nooshet.identity.dto.LoginRequest;
import org.nooshet.identity.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
