package com.ridelink.fare.dto;

import jakarta.validation.constraints.NotBlank;

/** Public estimate request: just two addresses. */
public class FareEstimateRequest {
    @NotBlank(message = "pickup address is required") private String pickup;
    @NotBlank(message = "destination address is required") private String destination;

    public String getPickup() { return pickup; }
    public void setPickup(String pickup) { this.pickup = pickup; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}
