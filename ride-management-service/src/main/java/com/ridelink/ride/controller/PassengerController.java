package com.ridelink.ride.controller;

import com.ridelink.ride.dto.*;
import com.ridelink.ride.security.CurrentUser;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Passenger (PASSENGER only)", description = "Passenger self-service: estimate, request, list and cancel my rides")
public class PassengerController {

    private final RideService rideService;

    public PassengerController(RideService rideService) { this.rideService = rideService; }

    @PostMapping("/api/rides/estimate")
    @Operation(summary = "Estimate distance and fare from addresses without creating a ride")
    public ResponseEntity<EstimateRideResponse> estimate(@Valid @RequestBody EstimateRideRequest request) {
        return ResponseEntity.ok(rideService.estimate(request));
    }

    @PostMapping("/api/{userId}/rides")
    @Operation(summary = "Create a ride from addresses only: geocodes both addresses, calculates distance and "
            + "estimated fare, then opens the ride to every eligible driver in the service area")
    public ResponseEntity<RideResponse> create(@PathVariable("userId") String userId,
                                               @Valid @RequestBody CreateRideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rideService.createRide(userId, request, CurrentUser.id()));
    }

    @GetMapping("/api/{userId}/rides")
    @Operation(summary = "List my own rides")
    public ResponseEntity<List<RideResponse>> myRides(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(rideService.getPassengerRides(userId, CurrentUser.id()));
    }

    @PutMapping("/api/{userId}/rides/{id}/cancel")
    @Operation(summary = "Cancel my own ride (only while it is REQUESTED, ASSIGNED, ACCEPTED or IN_PROGRESS)")
    public ResponseEntity<RideResponse> cancel(@PathVariable("userId") String userId,
                                               @PathVariable("id") String id,
                                               @Valid @RequestBody CancelRideRequest request) {
        return ResponseEntity.ok(rideService.cancelForPassenger(userId, id, request, CurrentUser.id()));
    }
}
