package com.ridelink.ride.exception;

public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(String id) {
        super("Ride not found: " + id);
    }
}
