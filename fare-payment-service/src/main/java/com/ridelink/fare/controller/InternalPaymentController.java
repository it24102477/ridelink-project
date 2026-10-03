package com.ridelink.fare.controller;

import com.ridelink.fare.dto.CreatePaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Server-to-server only. ride-management-service records the payment here when it completes a
 * ride itself; it does not forward a user token downstream, so this endpoint stays open (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Internal (service-to-service)", description = "Called by ride-management-service, not by end users")
public class InternalPaymentController {

    private final PaymentService paymentService;

    public InternalPaymentController(PaymentService paymentService) { this.paymentService = paymentService; }

    @PostMapping
    @Operation(summary = "Record a simulated payment for a completed ride (called by ride-management-service)")
    public ResponseEntity<PaymentResponse> record(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.recordPayment(request));
    }
}
