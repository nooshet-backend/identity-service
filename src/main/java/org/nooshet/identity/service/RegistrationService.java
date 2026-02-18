package org.nooshet.identity.service;

import org.nooshet.identity.dto.RegisterCompleteRequest;
import org.nooshet.identity.dto.LoginResponse;
import org.nooshet.identity.dto.OtpSendResponse;
import org.nooshet.identity.dto.OtpVerifyRequest;
import org.nooshet.identity.dto.OtpVerifyResponse;
import org.nooshet.identity.dto.RegisterRequest;

public interface RegistrationService {
    OtpSendResponse startRegistration(RegisterRequest request);
    OtpVerifyResponse verifyRegistrationOtp(OtpVerifyRequest request);
    LoginResponse completeRegistration(RegisterCompleteRequest request);
}
