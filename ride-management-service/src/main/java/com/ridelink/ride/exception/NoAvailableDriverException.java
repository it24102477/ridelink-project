package com.ridelink.ride.exception;

public class NoAvailableDriverException extends RuntimeException {
    public NoAvailableDriverException(String serviceArea) {
        super("No available driver found in service area: " + serviceArea);
    }
}
