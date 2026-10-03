package com.ridelink.fare.dto;

import jakarta.validation.constraints.NotNull;

public class FareEstimateByCoordinatesRequest {
    @NotNull private Double pickupLat;
    @NotNull private Double pickupLng;
    @NotNull private Double destinationLat;
    @NotNull private Double destinationLng;

    public FareEstimateByCoordinatesRequest() {}
    public FareEstimateByCoordinatesRequest(double pickupLat, double pickupLng, double destinationLat, double destinationLng) {
        this.pickupLat = pickupLat; this.pickupLng = pickupLng;
        this.destinationLat = destinationLat; this.destinationLng = destinationLng;
    }

    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }
    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }
    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }
}
