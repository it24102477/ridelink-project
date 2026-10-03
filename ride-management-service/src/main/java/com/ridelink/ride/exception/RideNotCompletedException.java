package com.ridelink.ride.exception;

import com.ridelink.ride.model.RideStatus;

/** Thrown when a ride is deleted before it has reached COMPLETED or CANCELLED. */
public class RideNotCompletedException extends RuntimeException {
    public RideNotCompletedException(RideStatus status) {
        super("Ride not completed. Only COMPLETED or CANCELLED rides can be deleted (current status: " + status + ")");
    }
}
