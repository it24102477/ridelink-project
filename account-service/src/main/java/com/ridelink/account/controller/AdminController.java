package com.ridelink.account.controller;

import com.ridelink.account.dto.UpdateStatusRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/accounts")
@Tag(name = "Admin", description = "Admin-only account management")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Admin: list all accounts")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll(currentRole()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Admin: get account details by id")
    public ResponseEntity<UserResponse> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(userService.getById(id, currentRole()));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Admin: suspend, reactivate or deactivate an account")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable("id") String id,
                                                       @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(userService.updateStatus(id, request, currentRole()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Admin: delete a PASSENGER or DRIVER account. Allowed only when every ride of that user is "
            + "COMPLETED or CANCELLED (otherwise: Can not delete his ride not completed). "
            + "A DRIVER's driver profile is deleted with the account")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable("id") String id,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        userService.deleteAccountAsAdmin(id, authorizationHeader, currentRole());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    private String currentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse(null);
    }
}