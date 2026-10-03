package com.ridelink.ride.exception;

import com.ridelink.ride.model.RideStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(RideStatus from, RideStatus to) {
        super("Cannot transition ride from " + from + " to " + to);
    }
}
