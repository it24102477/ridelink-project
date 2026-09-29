package com.ridelink.driver.controller;

import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/drivers")
@Tag(name = "Admin (ADMIN only)", description = "Administrative driver operations")
public class AdminController {

    private final DriverService driverService;

    public AdminController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    @Operation(summary = "List all driver profiles")
    public ResponseEntity<List<DriverResponse>> listAll() {
        return ResponseEntity.ok(driverService.listAll());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a driver profile. Allowed only when every ride the driver is on is "
            + "COMPLETED or CANCELLED (otherwise: Can not delete his ride not completed)")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable("id") String id,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        driverService.deleteProfileAsAdmin(id, authorizationHeader);
        return ResponseEntity.ok(Map.of("message", "Driver profile deleted successfully"));
    }
}
