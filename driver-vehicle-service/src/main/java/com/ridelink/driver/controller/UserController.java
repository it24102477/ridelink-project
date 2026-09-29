package com.ridelink.driver.controller;

import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "User (PASSENGER, DRIVER, ADMIN)", description = "Read-only driver lookups")
public class UserController {

    private final DriverService driverService;

    public UserController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a driver profile by id")
    public ResponseEntity<DriverResponse> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(driverService.getById(id));
    }

    @GetMapping("/available")
    @Operation(summary = "List available drivers, optionally filtered by service area")
    public ResponseEntity<List<DriverResponse>> available(
            @RequestParam(name = "serviceArea", required = false) String serviceArea) {
        return ResponseEntity.ok(driverService.findAvailableDrivers(serviceArea));
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete the driver profile that belongs to an account (DRIVER for their own, or ADMIN). "
            + "Called by account-service when a DRIVER account is deleted; does nothing if there is no profile")
    public ResponseEntity<Void> deleteByUserId(
            @PathVariable("userId") String userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replace("ROLE_", "")).orElse(null);
        driverService.deleteProfileByUserId(userId, authorizationHeader, (String) auth.getPrincipal(), role);
        return ResponseEntity.noContent().build();
    }
}
