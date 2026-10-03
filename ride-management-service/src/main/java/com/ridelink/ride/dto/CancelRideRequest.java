package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelRideRequest {
    @NotBlank private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
