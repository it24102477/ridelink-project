package com.ridelink.account.dto;

import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.User;

import java.time.Instant;

/** Never exposes the password hash. */
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private AccountStatus status;
    private Instant createdAt;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.fullName = u.getFullName();
        r.email = u.getEmail();
        r.phoneNumber = u.getPhoneNumber();
        r.role = u.getRole();
        r.status = u.getStatus();
        r.createdAt = u.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public Role getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
