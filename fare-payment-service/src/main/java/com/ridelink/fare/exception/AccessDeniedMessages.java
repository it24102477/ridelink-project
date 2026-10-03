package com.ridelink.fare.exception;

/** The one message every unauthorized / forbidden response from fare-payment-service carries. */
public final class AccessDeniedMessages {
    public static final String NOT_PERMITTED = "You are not permitted to perform that action";
    private AccessDeniedMessages() {}
}
