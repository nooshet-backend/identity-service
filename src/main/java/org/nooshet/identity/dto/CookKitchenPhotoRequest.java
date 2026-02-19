package org.nooshet.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(description = "Request for Home Cook kitchen photo upload")
public class CookKitchenPhotoRequest {
    @NotNull(message = "Kitchen photo is required")
    private MultipartFile kitchenPhoto;
    // Add more fields if multiple photos are needed
}

