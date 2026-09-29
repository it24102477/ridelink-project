package com.ridelink.account.exception;

public class RoleNotAllowedException extends RuntimeException {
    public RoleNotAllowedException() {
        super("Can not register that role");
    }

    public RoleNotAllowedException(String message) {
        super(message);
    }
}
