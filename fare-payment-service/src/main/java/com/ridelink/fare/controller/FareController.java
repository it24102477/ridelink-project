package com.ridelink.fare.controller;

import com.ridelink.fare.dto.*;
import com.ridelink.fare.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fares")
@Tag(name = "Fares", description = "Fare estimation and final fare calculation")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) { this.fareService = fareService; }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate a fare from a pickup address and a destination address. "
            + "Coordinates and road distance are calculated automatically (free OpenStreetMap APIs)")
    public ResponseEntity<AddressFareEstimateResponse> estimate(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareService.estimateByAddress(request));
    }

    @PostMapping("/estimate-coordinates")
    @Operation(summary = "Estimate a fare from coordinates (server-to-server, used by ride-management-service)")
    public ResponseEntity<FareEstimateResponse> estimateByCoordinates(@Valid @RequestBody FareEstimateByCoordinatesRequest request) {
        return ResponseEntity.ok(fareService.estimate(request));
    }

    @PostMapping("/final")
    @Operation(summary = "Calculate the final fare for a completed ride (called by ride-management-service)")
    public ResponseEntity<FinalFareResponse> finalFare(@Valid @RequestBody FinalFareRequest request) {
        return ResponseEntity.ok(fareService.finalFare(request));
    }
}
