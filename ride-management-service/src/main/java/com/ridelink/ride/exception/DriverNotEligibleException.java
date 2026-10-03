package com.ridelink.ride.exception;

/** Thrown when the driver attempting to accept a ride is not currently eligible to -
 *  e.g. not marked AVAILABLE, or not registered in the ride's service area. */
public class DriverNotEligibleException extends RuntimeException {
    public DriverNotEligibleException(String message) {
        super(message);
    }
}
