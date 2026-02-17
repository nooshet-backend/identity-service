package org.nooshet.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
