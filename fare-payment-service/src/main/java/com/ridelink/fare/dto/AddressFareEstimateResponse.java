package com.ridelink.fare.dto;

public class AddressFareEstimateResponse {
    public static class Point {
        private final String address; private final double lat; private final double lng;
        public Point(String address, double lat, double lng) { this.address = address; this.lat = lat; this.lng = lng; }
        public String getAddress() { return address; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }

    private final Point pickup;
    private final Point destination;
    private final double distanceKm;
    private final double estimatedFare;
    private final String currency;

    public AddressFareEstimateResponse(Point pickup, Point destination, double distanceKm,
                                       double estimatedFare, String currency) {
        this.pickup = pickup; this.destination = destination;
        this.distanceKm = distanceKm; this.estimatedFare = estimatedFare; this.currency = currency;
    }

    public Point getPickup() { return pickup; }
    public Point getDestination() { return destination; }
    public double getDistanceKm() { return distanceKm; }
    public double getEstimatedFare() { return estimatedFare; }
    public String getCurrency() { return currency; }
}
