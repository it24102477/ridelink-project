package com.ridelink.fare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class FinalFareRequest {
    @NotBlank private String rideId;
    @NotNull private Double pickupLat;
    @NotNull private Double pickupLng;
    @NotNull private Double destinationLat;
    @NotNull private Double destinationLng;
    @PositiveOrZero private double actualDurationMinutes;

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }
    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }
    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }
    public double getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(double actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }
}
