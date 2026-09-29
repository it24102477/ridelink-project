package com.ridelink.driver.model;

/** A driver's current location: the address they entered plus the coordinates resolved from it. */
public class GeoPoint {
    private String address;
    private double lat;
    private double lng;
    /** District the address falls in (one of Sri Lanka's 25), resolved by the geocoder; may be null. */
    private String district;

    public GeoPoint() {}
    public GeoPoint(String address, double lat, double lng) {
        this.address = address;
        this.lat = lat;
        this.lng = lng;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}
