package com.ridelink.ride.client;

/** Response shape returned by driver-vehicle-service's GET /api/drivers/{id}.
 *  Used on the accept path to verify that the driverId in the request really
 *  belongs to the calling driver and is currently eligible to accept a ride. */
public class DriverProfileDto {
    private String id;
    private String userId;
    private String serviceArea;
    private String availability;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}
