package com.ridelink.driver.exception;

public class DriverNotFoundException extends RuntimeException {
    public DriverNotFoundException(String id) {
        super("Driver not found: " + id);
    }
}
