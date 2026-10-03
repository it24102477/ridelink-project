package com.ridelink.fare.exception;

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(String address) {
        super("Could not find a location for address: " + address);
    }
}
