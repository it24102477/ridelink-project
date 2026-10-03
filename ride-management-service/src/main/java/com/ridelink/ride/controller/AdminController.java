package com.ridelink.ride.controller;

import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rides")
@Tag(name = "Admin (ADMIN only)", description = "Administrative ride operations")
public class AdminController {

    private final RideService rideService;

    public AdminController(RideService rideService) { this.rideService = rideService; }

    @GetMapping
    @Operation(summary = "List all rides (only ADMIN can list every ride)")
    public ResponseEntity<List<RideResponse>> listAll() {
        return ResponseEntity.ok(rideService.listAll());
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a ride. Allowed only from REQUESTED, ASSIGNED, ACCEPTED or IN_PROGRESS")
    public ResponseEntity<RideResponse> cancel(@PathVariable("id") String id,
                                               @Valid @RequestBody CancelRideRequest request) {
        return ResponseEntity.ok(rideService.cancelAsAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a ride. Allowed only when its status is COMPLETED or CANCELLED")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("id") String id) {
        rideService.deleteRide(id);
        return ResponseEntity.ok(Map.of("message", "Ride deleted successfully"));
    }
}
