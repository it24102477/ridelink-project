package com.ridelink.fare.controller;

import com.ridelink.fare.dto.CompleteRidePaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.security.CurrentUser;
import com.ridelink.fare.service.PaymentService;
import com.ridelink.fare.service.RideCompletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Driver (DRIVER only)", description = "Complete a ride and get paid; a driver's own payments")
public class DriverController {

    private final PaymentService paymentService;
    private final RideCompletionService rideCompletionService;

    public DriverController(PaymentService paymentService, RideCompletionService rideCompletionService) {
        this.paymentService = paymentService;
        this.rideCompletionService = rideCompletionService;
    }

    @PostMapping("/api/payments/rides/{rideId}/{driverId}/Payment")
    @Operation(summary = "Driver creates the payment for a ride, then the ride is completed automatically: "
            + "requires a DRIVER-role token, checks that the given driverId is the one ride-management-service "
            + "(localhost:8083) has on record for rideId and that the ride is IN_PROGRESS (i.e. already started), "
            + "records the final payment here, then asks ride-management-service to mark the ride COMPLETED")
    public ResponseEntity<PaymentResponse> completeRideAndPay(
            @PathVariable("rideId") String rideId,
            @PathVariable("driverId") String driverId,
            @Valid @RequestBody CompleteRidePaymentRequest request,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rideCompletionService.completeRideAndPay(rideId, driverId, request, authorizationHeader, CurrentUser.id()));
    }

    @GetMapping("/api/driver/{userId}/payments")
    @Operation(summary = "List my own payments (rides I carried)")
    public ResponseEntity<List<PaymentResponse>> myPayments(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(paymentService.getDriverPayments(userId, CurrentUser.id()));
    }

    @GetMapping("/api/driver/{userId}/payments/{id}")
    @Operation(summary = "Get one of my own payments by payment id")
    public ResponseEntity<PaymentResponse> myPaymentById(@PathVariable("userId") String userId,
                                                         @PathVariable("id") String id) {
        return ResponseEntity.ok(paymentService.getDriverPaymentById(userId, id, CurrentUser.id()));
    }

    @GetMapping("/api/driver/{userId}/payments/ride/{rideId}")
    @Operation(summary = "Get my own payment for a ride, by ride id")
    public ResponseEntity<PaymentResponse> myPaymentByRide(@PathVariable("userId") String userId,
                                                           @PathVariable("rideId") String rideId) {
        return ResponseEntity.ok(paymentService.getDriverPaymentByRide(userId, rideId, CurrentUser.id()));
    }
}
