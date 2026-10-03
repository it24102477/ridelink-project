package com.ridelink.fare.controller;

import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.security.CurrentUser;
import com.ridelink.fare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "User (PASSENGER, DRIVER, ADMIN)", description = "Payment lookups shared by all roles (own payments only, ADMIN any)")
public class UserController {

    private final PaymentService paymentService;

    public UserController(PaymentService paymentService) { this.paymentService = paymentService; }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by id (its passenger, its driver, or ADMIN)")
    public ResponseEntity<PaymentResponse> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(paymentService.getById(id, CurrentUser.id(), CurrentUser.role()));
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Get the receipt for a completed payment (its passenger, its driver, or ADMIN)")
    public ResponseEntity<PaymentResponse> receipt(@PathVariable("id") String id) {
        return ResponseEntity.ok(paymentService.getReceipt(id, CurrentUser.id(), CurrentUser.role()));
    }
}
