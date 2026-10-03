package com.ridelink.fare.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator(300.0, 80.0, 10.0);

    @Test
    void distanceKm_isZero_forSamePoint() {
        double d = calculator.distanceKm(6.9271, 79.8612, 6.9271, 79.8612);
        assertEquals(0.0, d, 0.0001);
    }

    @Test
    void distanceKm_isPositive_forDifferentPoints() {
        // Colombo to Negombo, roughly ~35km apart
        double d = calculator.distanceKm(6.9271, 79.8612, 7.2083, 79.8358);
        assertTrue(d > 25 && d < 45, "expected distance in a plausible range, got " + d);
    }

    @Test
    void calculateFare_appliesDocumentedRule() {
        // fare = base + perKm*distance + perMin*duration
        double fare = calculator.calculateFare(10.0, 20.0);
        assertEquals(300.0 + 80.0 * 10.0 + 10.0 * 20.0, fare, 0.001);
    }

    @Test
    void estimatedDurationMinutes_usesAssumedAverageSpeed() {
        double duration = calculator.estimatedDurationMinutes(30.0); // 30km at 30km/h = 60 min
        assertEquals(60.0, duration, 0.001);
    }
}
