package com.ridelink.ride.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** The authenticated caller, taken from the JWT the filter put in the security context. */
public final class CurrentUser {
    private CurrentUser() {}

    public static String id() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static String role() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse(null);
    }
}
