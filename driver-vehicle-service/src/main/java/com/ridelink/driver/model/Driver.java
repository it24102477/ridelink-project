package com.ridelink.driver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "drivers")
public class Driver {

    @Id
    private String id;

    /** Foreign reference to the account-service User id. No cross-service DB access. */
    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String licenseNumber;
    private Vehicle vehicle;
    private String serviceArea;
    private GeoPoint currentLocation;
    private AvailabilityStatus availability = AvailabilityStatus.OFFLINE;
    private double rating = 5.0;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Driver() {}

    public Driver(String userId, String licenseNumber, Vehicle vehicle, String serviceArea) {
        this.userId = userId;
        this.licenseNumber = licenseNumber;
        this.vehicle = vehicle;
        this.serviceArea = serviceArea;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
    public GeoPoint getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(GeoPoint currentLocation) { this.currentLocation = currentLocation; }
    public AvailabilityStatus getAvailability() { return availability; }
    public void setAvailability(AvailabilityStatus availability) { this.availability = availability; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
