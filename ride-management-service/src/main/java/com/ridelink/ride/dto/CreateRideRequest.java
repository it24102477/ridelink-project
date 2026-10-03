package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Only two inputs. The passenger comes from the URL (/api/{userId}/rides); coordinates,
 * the service area (district), distance and estimated fare are all calculated automatically
 * from the two addresses.
 */
public class CreateRideRequest {
    @NotBlank(message = "pickup address is required") private String pickup;
    @NotBlank(message = "destination address is required") private String destination;

    public String getPickup() { return pickup; }
    public void setPickup(String pickup) { this.pickup = pickup; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}
