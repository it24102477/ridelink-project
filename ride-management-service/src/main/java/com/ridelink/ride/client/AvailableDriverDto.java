package com.ridelink.ride.client;

/** Response shape returned by driver-vehicle-service's GET /api/drivers/available. */
public class AvailableDriverDto {
    private String id;
    private String userId;
    private String serviceArea;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
}
