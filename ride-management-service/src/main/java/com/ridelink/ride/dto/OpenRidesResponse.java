package com.ridelink.ride.dto;

/**
 * Whether a user (passenger or driver) still has rides that are not COMPLETED or CANCELLED.
 * Used by account-service and driver-vehicle-service before they delete an account / profile.
 */
public class OpenRidesResponse {
    private final String userId;
    private final long openRideCount;
    private final boolean hasOpenRides;

    public OpenRidesResponse(String userId, long openRideCount) {
        this.userId = userId;
        this.openRideCount = openRideCount;
        this.hasOpenRides = openRideCount > 0;
    }

    public String getUserId() { return userId; }
    public long getOpenRideCount() { return openRideCount; }
    public boolean isHasOpenRides() { return hasOpenRides; }
}
