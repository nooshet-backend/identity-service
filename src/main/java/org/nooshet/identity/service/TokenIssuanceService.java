package org.nooshet.identity.service;

import org.nooshet.identity.entity.User;
import org.nooshet.identity.dto.LoginResponse;

public interface TokenIssuanceService {
    LoginResponse issueTokens(User user);
}

