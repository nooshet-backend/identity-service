package org.nooshet.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request for Buyer rules acceptance")
public class BuyerRulesAcceptanceRequest {
    @NotNull(message = "Rules must be accepted")
    private Boolean accepted;
}

