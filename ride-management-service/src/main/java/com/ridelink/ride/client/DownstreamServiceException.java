package com.ridelink.ride.client;

public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String serviceName, Throwable cause) {
        super("Could not reach " + serviceName, cause);
    }
}
