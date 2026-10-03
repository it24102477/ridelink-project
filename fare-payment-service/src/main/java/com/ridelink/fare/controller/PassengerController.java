package com.ridelink.fare.controller;

import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.security.CurrentUser;
import com.ridelink.fare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passenger/{userId}/payments")
@Tag(name = "Passenger (PASSENGER only)", description = "A passenger's own payments")
public class PassengerController {

    private final PaymentService paymentService;

    public PassengerController(PaymentService paymentService) { this.paymentService = paymentService; }

    @GetMapping
    @Operation(summary = "List my own payments")
    public ResponseEntity<List<PaymentResponse>> myPayments(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(paymentService.getPassengerPayments(userId, CurrentUser.id()));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get my own payment for a ride, by ride id")
    public ResponseEntity<PaymentResponse> myPaymentByRide(@PathVariable("userId") String userId,
                                                           @PathVariable("rideId") String rideId) {
        return ResponseEntity.ok(paymentService.getPassengerPaymentByRide(userId, rideId, CurrentUser.id()));
    }
}
