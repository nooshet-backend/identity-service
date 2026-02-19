package org.nooshet.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request for Courier registration")
public class CourierRegistrationRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    @NotBlank(message = "FIN code is required")
    private String finCode;
    @NotBlank(message = "Transport type is required")
    private String transportType; // car, motorcycle, bicycle, foot
    private String licensePlate;
    private String vehicleMake;
    private String vehicleModel;
    private String licenseDocument;
    // Only required for car/motorcycle
}

