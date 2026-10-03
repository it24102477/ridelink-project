package com.ridelink.ride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ride Management Service
 * Owner: Member 3
 * Responsibility: ride request creation, pickup/destination details, driver
 * assignment, ride status lifecycle (requested, assigned, accepted, in-progress,
 * completed, cancelled), ride retrieval.
 */
@SpringBootApplication
public class RideManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RideManagementServiceApplication.class, args);
    }
}
