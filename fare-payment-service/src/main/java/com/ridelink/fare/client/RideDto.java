package com.ridelink.fare.client;

/** Response shape returned by ride-management-service's GET /api/rides/{id} and
 * PUT /api/rides/{id}/complete (RideResponse) - only the fields fare-payment-service
 * needs to authenticate the caller against the ride and compute the final fare. */
public class RideDto {
    private String id;
    private String passengerId;
    private String driverId;
    private LocationDto pickup;
    private LocationDto destination;
    private String status;
    private Double finalFare;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public LocationDto getPickup() { return pickup; }
    public void setPickup(LocationDto pickup) { this.pickup = pickup; }
    public LocationDto getDestination() { return destination; }
    public void setDestination(LocationDto destination) { this.destination = destination; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getFinalFare() { return finalFare; }
    public void setFinalFare(Double finalFare) { this.finalFare = finalFare; }
}
