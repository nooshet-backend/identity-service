package org.nooshet.identity.service;

import org.nooshet.identity.entity.User;

public interface UserService {
    User createNewUser(String firstName, String lastName, String passwordHash, String phone, String role, String email);
    User completeUserRegistration(String registrationToken, String role);
    void markSetupComplete(Long userId);
}
