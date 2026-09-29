package com.ridelink.driver.exception;

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(String address) {
        super("Could not find a location for address: " + address);
    }
}
