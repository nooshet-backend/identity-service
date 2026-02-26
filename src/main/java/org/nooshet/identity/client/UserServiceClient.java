package org.nooshet.identity.client;

import org.nooshet.identity.dto.ApiResponse;
import org.nooshet.identity.dto.CreateProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${application.config.user-service-url}")
public interface UserServiceClient {

    @PostMapping("/api/v1/internal/profiles")
    ApiResponse<String> createProfile(@RequestBody CreateProfileRequest request, @RequestHeader("X-Internal-Secret") String secret);

    @org.springframework.web.bind.annotation.DeleteMapping("/api/v1/internal/profiles/{userId}")
    ApiResponse<String> deleteProfile(@org.springframework.web.bind.annotation.PathVariable("userId") Long userId, @RequestHeader("X-Internal-Secret") String secret);
}
