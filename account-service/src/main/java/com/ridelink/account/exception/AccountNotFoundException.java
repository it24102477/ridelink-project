package com.ridelink.account.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String id) {
        super("Account not found: " + id);
    }
}
