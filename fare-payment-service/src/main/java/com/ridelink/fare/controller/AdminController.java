package com.ridelink.fare.controller;

import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "Admin (ADMIN only)", description = "Administrative payment operations")
public class AdminController {

    private final PaymentService paymentService;

    public AdminController(PaymentService paymentService) { this.paymentService = paymentService; }

    @GetMapping
    @Operation(summary = "List all payments (only ADMIN can list every payment)")
    public ResponseEntity<List<PaymentResponse>> listAll() {
        return ResponseEntity.ok(paymentService.listAll());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payment (only ADMIN can delete a payment)")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("id") String id) {
        paymentService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Payment deleted successfully"));
    }
}
