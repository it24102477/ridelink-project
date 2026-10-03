package com.ridelink.ride.dto;

import com.ridelink.ride.model.Location;

public class EstimateRideResponse {
    private final Location pickup;
    private final Location destination;
    private final double distanceKm;
    private final double estimatedFare;
    private final String currency;

    public EstimateRideResponse(Location pickup, Location destination, double distanceKm,
                                double estimatedFare, String currency) {
        this.pickup = pickup; this.destination = destination;
        this.distanceKm = distanceKm; this.estimatedFare = estimatedFare; this.currency = currency;
    }

    public Location getPickup() { return pickup; }
    public Location getDestination() { return destination; }
    public double getDistanceKm() { return distanceKm; }
    public double getEstimatedFare() { return estimatedFare; }
    public String getCurrency() { return currency; }
}
