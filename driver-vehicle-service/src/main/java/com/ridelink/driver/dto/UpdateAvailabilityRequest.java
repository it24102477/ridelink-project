package com.ridelink.driver.dto;

import com.ridelink.driver.model.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateAvailabilityRequest {
    @NotNull(message = "Availability is required (AVAILABLE, BUSY or OFFLINE)")
    private AvailabilityStatus availability;

    public AvailabilityStatus getAvailability() { return availability; }
    public void setAvailability(AvailabilityStatus availability) { this.availability = availability; }
}