package org.nooshet.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request for Home Cook rules acceptance")
public class CookRulesAcceptanceRequest {
    @NotNull(message = "Rules must be accepted")
    private Boolean accepted;
    // Backend should enforce that user scrolled/read to the end before accepting
}

