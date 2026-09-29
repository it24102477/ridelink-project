package com.ridelink.driver.exception;

/** Another service (or the geocoder) could not be reached or answered with an error. */
public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String service, Throwable cause) {
        super(service + " is unavailable, please try again later", cause);
    }
}
