package org.nooshet.identity.service;

import org.nooshet.identity.constants.OtpPurpose;
import org.nooshet.identity.dto.OtpSendRequest;
import org.nooshet.identity.dto.OtpSendResponse;
import org.nooshet.identity.dto.OtpVerifyRequest;
import org.nooshet.identity.dto.OtpVerifyResponse;

public interface OtpService {
    OtpSendResponse sendOtp(OtpSendRequest request);
    OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request);
}
