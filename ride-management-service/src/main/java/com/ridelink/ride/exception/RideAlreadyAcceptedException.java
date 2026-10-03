package com.ridelink.ride.exception;

/** Thrown when a driver tries to accept a ride that is no longer open - either another
 *  driver already claimed it (won the accept race), or it moved on/was cancelled. */
public class RideAlreadyAcceptedException extends RuntimeException {
    public RideAlreadyAcceptedException(String rideId) {
        super("Ride " + rideId + " is no longer available to accept - another driver already claimed it, "
                + "or it is no longer in REQUESTED state");
    }
}
