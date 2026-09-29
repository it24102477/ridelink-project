package com.ridelink.driver.exception;

/** The driver still has a ride that is not COMPLETED or CANCELLED, so the profile can not be deleted. */
public class RideNotCompletedException extends RuntimeException {
    public RideNotCompletedException(String message) {
        super(message);
    }
}
