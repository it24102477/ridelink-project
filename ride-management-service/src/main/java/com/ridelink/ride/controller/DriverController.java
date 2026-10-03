package com.ridelink.ride.controller;

import com.ridelink.ride.dto.*;
import com.ridelink.ride.security.CurrentUser;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Driver (DRIVER only)", description = "Driver actions on rides: accept, start, complete, cancel, my rides")
public class DriverController {

    private final RideService rideService;

    public DriverController(RideService rideService) { this.rideService = rideService; }

    @PutMapping("/api/rides/{id}/{driverId}/accept")
    @Operation(summary = "Claim an open (REQUESTED) ride. Any eligible driver may call this, but only the first "
            + "to land wins the ride (everyone else gets a 409). The winning driver is set to BUSY automatically")
    public ResponseEntity<RideResponse> accept(@PathVariable("id") String id,
                                               @PathVariable("driverId") String driverId,
                                               @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(rideService.accept(id, driverId, CurrentUser.id(), authorizationHeader));
    }

    @PutMapping("/api/rides/{id}/start")
    @Operation(summary = "Start an accepted ride")
    public ResponseEntity<RideResponse> start(@PathVariable("id") String id) {
        return ResponseEntity.ok(rideService.start(id, CurrentUser.id()));
    }

    @PutMapping("/api/rides/{id}/complete")
    @Operation(summary = "Marks a PAID ride COMPLETED (no request body; the fare is taken from the payment). "
            + "Rejected unless the ride was first paid through POST http://localhost:8084/api/payments/rides/{rideId}/{driverId}/Payment, "
            + "which calls this automatically")
    public ResponseEntity<RideResponse> complete(@PathVariable("id") String id,
                                                 @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(rideService.complete(id, CurrentUser.id(), authorizationHeader));
    }

    @PutMapping("/api/rides/{id}/cancel")
    @Operation(summary = "Cancel a ride I was assigned to (only while it has not completed)")
    public ResponseEntity<RideResponse> cancel(@PathVariable("id") String id,
                                               @Valid @RequestBody CancelRideRequest request) {
        return ResponseEntity.ok(rideService.cancel(id, request, CurrentUser.id()));
    }

    @GetMapping("/api/rides/driver/{userId}")
    @Operation(summary = "List my own rides (the rides I accepted)")
    public ResponseEntity<List<RideResponse>> myRides(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(rideService.getDriverRides(userId, CurrentUser.id()));
    }
}
