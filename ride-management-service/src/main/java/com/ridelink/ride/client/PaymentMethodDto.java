package com.ridelink.ride.client;

/** Mirrors fare-payment-service's PaymentMethod enum. Duplicated intentionally: each
 * service owns its own contract types and must not share a database or a Java model
 * class with another service (per the brief's "no cross-service DB/table access" rule). */
public enum PaymentMethodDto {
    SIMULATED_CARD,
    SIMULATED_CASH,
    SIMULATED_WALLET
}
