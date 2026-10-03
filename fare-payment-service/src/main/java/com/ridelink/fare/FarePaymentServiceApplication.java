package com.ridelink.fare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fare & Payment Service
 * Owner: Member 4
 * Responsibility: fare estimation, final fare calculation using a documented rule,
 * simulated payment recording, payment status, receipt generation and retrieval.
 */
@SpringBootApplication
public class FarePaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarePaymentServiceApplication.class, args);
    }
}
