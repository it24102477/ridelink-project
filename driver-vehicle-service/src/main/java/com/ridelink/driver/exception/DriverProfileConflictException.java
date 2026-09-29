package com.ridelink.driver.exception;

/**
 * Thrown when a new driver profile would duplicate a value that must be unique
 * across all drivers (currently: license number and vehicle plate number).
 */
public class DriverProfileConflictException extends RuntimeException {
    public DriverProfileConflictException(String message) {
        super(message);
    }

    public static DriverProfileConflictException duplicateLicenseNumber(String licenseNumber) {
        return new DriverProfileConflictException(
                "A driver profile already exists with license number: " + licenseNumber);
    }

    public static DriverProfileConflictException duplicatePlateNumber(String plateNumber) {
        return new DriverProfileConflictException(
                "A driver profile already exists with vehicle plate number: " + plateNumber);
    }
}
