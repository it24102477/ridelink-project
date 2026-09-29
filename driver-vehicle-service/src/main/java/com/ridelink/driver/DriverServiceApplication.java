package com.ridelink.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Driver & Vehicle Service
 * Owner: Member 2
 * Responsibility: driver operational profile, vehicle details, availability status,
 * service area, simulated current location, retrieval of eligible available drivers.
 */
@SpringBootApplication
public class DriverServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DriverServiceApplication.class, args);
    }
}
