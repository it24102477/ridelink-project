package com.ridelink.account.exception;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException() {
        super("This account is not active");
    }
}
