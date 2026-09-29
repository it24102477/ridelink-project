package com.ridelink.driver.controller;

import com.ridelink.driver.dto.*;
import com.ridelink.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{userId}/drivers")
@Tag(name = "Driver (DRIVER only)", description = "Driver self-service: profile, vehicle, service area, availability, location")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @Operation(summary = "Create my driver profile")
    public ResponseEntity<DriverResponse> create(@PathVariable("userId") String userId,
                                                 @Valid @RequestBody CreateDriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(driverService.createProfile(userId, request, currentUserId()));
    }

    @PutMapping("/{id}/vehicle")
    @Operation(summary = "Update my vehicle (make, model, plateNumber, color, capacity)")
    public ResponseEntity<DriverResponse> updateVehicle(@PathVariable("userId") String userId,
                                                        @PathVariable("id") String id,
                                                        @Valid @RequestBody VehicleDto request) {
        return ResponseEntity.ok(driverService.updateVehicle(userId, id, request, currentUserId()));
    }

    @PutMapping("/{id}/service-area")
    @Operation(summary = "Update my service area (one of Sri Lanka's districts)")
    public ResponseEntity<DriverResponse> updateServiceArea(@PathVariable("userId") String userId,
                                                            @PathVariable("id") String id,
                                                            @Valid @RequestBody UpdateServiceAreaRequest request) {
        return ResponseEntity.ok(driverService.updateServiceArea(userId, id, request, currentUserId()));
    }

    @PutMapping("/{id}/availability")
    @Operation(summary = "Update my availability (AVAILABLE, BUSY, OFFLINE)")
    public ResponseEntity<DriverResponse> updateAvailability(@PathVariable("userId") String userId,
                                                             @PathVariable("id") String id,
                                                             @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(driverService.updateAvailability(userId, id, request, currentUserId()));
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "Update my simulated current location")
    public ResponseEntity<DriverResponse> updateLocation(@PathVariable("userId") String userId,
                                                         @PathVariable("id") String id,
                                                         @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(driverService.updateLocation(userId, id, request, currentUserId()));
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}