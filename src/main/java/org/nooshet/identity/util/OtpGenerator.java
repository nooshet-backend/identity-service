package org.nooshet.identity.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class OtpGenerator {

    private static final String DIGITS = "0123456789";
    private static final int OTP_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generateOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }
}
