package com.ridelink.fare.dto;

public class FareEstimateResponse {
    private double distanceKm;
    private double estimatedFare;
    private String currency;

    public FareEstimateResponse(double distanceKm, double estimatedFare, String currency) {
        this.distanceKm = distanceKm;
        this.estimatedFare = estimatedFare;
        this.currency = currency;
    }

    public double getDistanceKm() { return distanceKm; }
    public double getEstimatedFare() { return estimatedFare; }
    public String getCurrency() { return currency; }
}
