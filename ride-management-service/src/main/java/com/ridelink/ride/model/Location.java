package com.ridelink.ride.model;

public class Location {
    private double lat;
    private double lng;
    private String address;
    /** Sri Lanka district resolved from the address by the geocoder, or null if none could be determined. */
    private String district;

    public Location() {}
    public Location(double lat, double lng, String address) {
        this.lat = lat; this.lng = lng; this.address = address;
    }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
}
