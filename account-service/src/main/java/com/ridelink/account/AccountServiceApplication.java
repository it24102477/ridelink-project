package com.ridelink.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Account Service
 * Owner: Member 1
 * Responsibility: passenger/driver account registration, login & token issuance,
 * role management, profile viewing/updating, account status management.
 */
@SpringBootApplication
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
