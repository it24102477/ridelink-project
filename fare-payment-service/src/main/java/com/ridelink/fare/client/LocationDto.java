package com.ridelink.fare.client;

/** Mirrors ride-management-service's Location model as returned inside RideDto.
 * Duplicated intentionally: each service owns its own contract types. */
public class LocationDto {
    private double lat;
    private double lng;
    private String address;

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
