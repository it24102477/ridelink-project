package com.ridelink.ride.exception;

/** The pickup address could not be matched to one of Sri Lanka's 25 districts. */
public class ServiceAreaNotFoundException extends RuntimeException {
    public ServiceAreaNotFoundException(String pickupAddress) {
        super("Could not determine a Sri Lanka service area (district) for pickup address: " + pickupAddress);
    }
}
