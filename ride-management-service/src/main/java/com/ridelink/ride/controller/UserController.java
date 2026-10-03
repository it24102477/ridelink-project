package com.ridelink.ride.controller;

import com.ridelink.ride.dto.OpenRidesResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.security.CurrentUser;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rides")
@Tag(name = "User (PASSENGER, DRIVER, ADMIN)", description = "Read-only ride lookups shared by all roles")
public class UserController {

    private final RideService rideService;

    public UserController(RideService rideService) { this.rideService = rideService; }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ride by id (the ride's passenger, its assigned driver, or ADMIN)")
    public ResponseEntity<RideResponse> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(rideService.getById(id, CurrentUser.id(), CurrentUser.role()));
    }

    @GetMapping("/users/{userId}/open-rides")
    @Operation(summary = "Does this user still have rides that are not COMPLETED or CANCELLED? "
            + "(the user themself or ADMIN; used before an account or driver profile is deleted)")
    public ResponseEntity<OpenRidesResponse> openRides(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(rideService.getOpenRides(userId, CurrentUser.id(), CurrentUser.role()));
    }
}
