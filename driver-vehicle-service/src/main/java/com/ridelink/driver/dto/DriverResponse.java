package com.ridelink.driver.dto;

import com.ridelink.driver.model.AvailabilityStatus;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.model.GeoPoint;
import com.ridelink.driver.model.Vehicle;

public class DriverResponse {
    private String id;
    private String userId;
    private String licenseNumber;
    private Vehicle vehicle;
    private String serviceArea;
    private GeoPoint currentLocation;
    private AvailabilityStatus availability;
    private double rating;

    public static DriverResponse from(Driver d) {
        DriverResponse r = new DriverResponse();
        r.id = d.getId();
        r.userId = d.getUserId();
        r.licenseNumber = d.getLicenseNumber();
        r.vehicle = d.getVehicle();
        r.serviceArea = d.getServiceArea();
        r.currentLocation = d.getCurrentLocation();
        r.availability = d.getAvailability();
        r.rating = d.getRating();
        return r;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getLicenseNumber() { return licenseNumber; }
    public Vehicle getVehicle() { return vehicle; }
    public String getServiceArea() { return serviceArea; }
    public GeoPoint getCurrentLocation() { return currentLocation; }
    public AvailabilityStatus getAvailability() { return availability; }
    public double getRating() { return rating; }
}
