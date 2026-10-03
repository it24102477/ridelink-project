package com.ridelink.fare.dto;

public class FinalFareResponse {
    private String rideId;
    private double distanceKm;
    private double durationMinutes;
    private double finalFare;
    private String currency;

    public FinalFareResponse(String rideId, double distanceKm, double durationMinutes, double finalFare, String currency) {
        this.rideId = rideId;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.finalFare = finalFare;
        this.currency = currency;
    }

    public String getRideId() { return rideId; }
    public double getDistanceKm() { return distanceKm; }
    public double getDurationMinutes() { return durationMinutes; }
    public double getFinalFare() { return finalFare; }
    public String getCurrency() { return currency; }
}
