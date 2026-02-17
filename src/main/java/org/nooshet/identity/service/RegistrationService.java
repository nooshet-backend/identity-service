package org.nooshet.identity.service;

import org.nooshet.identity.dto.RegisterCompleteRequest;
import org.nooshet.identity.dto.LoginResponse;

public interface RegistrationService {
    LoginResponse completeRegistration(RegisterCompleteRequest request);
}
