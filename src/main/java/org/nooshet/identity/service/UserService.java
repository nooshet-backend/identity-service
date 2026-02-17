package org.nooshet.identity.service;

import org.nooshet.identity.entity.User;

public interface UserService {
    User createNewUser(String firstName, String lastName, String passwordHash, String phone, String userType, String email);
}

