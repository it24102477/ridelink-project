package com.ridelink.account.exception;

/** The account still has a ride that is not COMPLETED or CANCELLED, so it can not be deleted. */
public class RideNotCompletedException extends RuntimeException {
    public RideNotCompletedException(String message) {
        super(message);
    }
}
