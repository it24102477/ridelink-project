package com.ridelink.ride.dto;

import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;

import java.time.Instant;

public class RideResponse {
    private String id;
    private String passengerId;
    private String driverId;
    private Location pickup;
    private Location destination;
    private String serviceArea;
    private RideStatus status;
    private Double estimatedFare;
    private Double distanceKm;
    private Double finalFare;
    private String cancellationReason;
    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    public static RideResponse from(Ride r) {
        RideResponse res = new RideResponse();
        res.id = r.getId();
        res.passengerId = r.getPassengerId();
        res.driverId = r.getDriverId();
        res.pickup = r.getPickup();
        res.destination = r.getDestination();
        res.serviceArea = r.getServiceArea();
        res.status = r.getStatus();
        res.estimatedFare = r.getEstimatedFare();
        res.distanceKm = r.getDistanceKm();
        res.finalFare = r.getFinalFare();
        res.cancellationReason = r.getCancellationReason();
        res.requestedAt = r.getRequestedAt();
        res.assignedAt = r.getAssignedAt();
        res.acceptedAt = r.getAcceptedAt();
        res.startedAt = r.getStartedAt();
        res.completedAt = r.getCompletedAt();
        res.cancelledAt = r.getCancelledAt();
        return res;
    }

    public String getId() { return id; }
    public String getPassengerId() { return passengerId; }
    public String getDriverId() { return driverId; }
    public Location getPickup() { return pickup; }
    public Location getDestination() { return destination; }
    public String getServiceArea() { return serviceArea; }
    public RideStatus getStatus() { return status; }
    public Double getDistanceKm() { return distanceKm; }
    public Double getEstimatedFare() { return estimatedFare; }
    public Double getFinalFare() { return finalFare; }
    public String getCancellationReason() { return cancellationReason; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
