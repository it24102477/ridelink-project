package com.ridelink.account.controller;

import com.ridelink.account.dto.ChangePasswordRequest;
import com.ridelink.account.dto.UpdateProfileRequest;
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

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "My profile and password")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my own profile details")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.getMe(currentUserId()));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change my password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete my own account (PASSENGER or DRIVER). Allowed only when every ride of mine is "
            + "COMPLETED or CANCELLED (otherwise: Can not delete your ride not completed). "
            + "A DRIVER's driver profile is deleted with the account")
    public ResponseEntity<Map<String, String>> deleteMe(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        userService.deleteOwnAccount(currentUserId(), authorizationHeader, currentRole());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update own profile (name, phone number)")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable("id") String id,
                                                        @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(id, request, currentUserId(), currentRole()));
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String currentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse(null);
    }
}