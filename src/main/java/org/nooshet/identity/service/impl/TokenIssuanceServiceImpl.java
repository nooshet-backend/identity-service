package org.nooshet.identity.service.impl;

import org.nooshet.identity.dto.LoginResponse;
import org.nooshet.identity.entity.User;
import org.nooshet.identity.service.TokenIssuanceService;
import org.springframework.stereotype.Service;

@Service
public class TokenIssuanceServiceImpl implements TokenIssuanceService {

    @Override
    public LoginResponse issueTokens(User user) {
        // Minimal placeholder implementation - generate dummy tokens
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken("access-token-for-user-" + (user.getId() != null ? user.getId() : "new"));
        resp.setRefreshToken("refresh-token-for-user-" + (user.getId() != null ? user.getId() : "new"));
        return resp;
    }
}

