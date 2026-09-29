package com.ridelink.driver.exception;

/** Thrown when a driver sets a current location that is not inside their own service area (district). */
public class LocationOutsideServiceAreaException extends RuntimeException {
    public LocationOutsideServiceAreaException(String serviceArea) {
        super("Your location must be inside your service area (" + serviceArea + ")");
    }

    /** The address was found, but no district could be worked out for it, so it cannot be verified. */
    public static LocationOutsideServiceAreaException unverifiable(String serviceArea) {
        return new LocationOutsideServiceAreaException(
                "Your location must be inside your service area (" + serviceArea
                + "). We could not confirm which district that address is in - please enter a more specific address", true);
    }

    private LocationOutsideServiceAreaException(String message, boolean raw) {
        super(message);
    }
}
