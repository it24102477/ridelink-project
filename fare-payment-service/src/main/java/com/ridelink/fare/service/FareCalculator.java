package com.ridelink.fare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Documented fare rule (report section 6.2 / G3 evidence):
 *
 *   fare = baseFare + (perKmRate * distanceKm) + (perMinRate * durationMinutes)
 *
 * Distance between two simulated coordinates is computed with the Haversine
 * great-circle formula. For fare *estimates*, duration is approximated from
 * distance using an assumed average city speed of 30 km/h. For the *final*
 * fare, the actual ride duration reported by ride-management-service is used.
 */
@Component
public class FareCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double ASSUMED_AVG_SPEED_KMH = 30.0;

    private final double baseFare;
    private final double perKmRate;
    private final double perMinRate;

    public FareCalculator(@Value("${ridelink.fare.base-fare}") double baseFare,
                           @Value("${ridelink.fare.per-km-rate}") double perKmRate,
                           @Value("${ridelink.fare.per-min-rate}") double perMinRate) {
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.perMinRate = perMinRate;
    }

    public double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public double estimatedDurationMinutes(double distanceKm) {
        return (distanceKm / ASSUMED_AVG_SPEED_KMH) * 60.0;
    }

    public double calculateFare(double distanceKm, double durationMinutes) {
        double fare = baseFare + (perKmRate * distanceKm) + (perMinRate * durationMinutes);
        return Math.round(fare * 100.0) / 100.0;
    }
}
