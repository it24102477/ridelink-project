package com.ridelink.account.dto;

import com.ridelink.account.model.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

   
    @NotBlank(message = "fullName is required")
    @Pattern(
        regexp = "^[\\p{L}. ]+$",
        message = "can only use letters for full name"
    )
    @Schema(example = "Nimal Perera")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(
    regexp = "^[0-9]{10}$",
    message = "phoneNumber must contain exactly 10 numbers"
)
    private String phoneNumber;

    @NotNull(message = "role is required (PASSENGER or DRIVER)")
    private Role role;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
