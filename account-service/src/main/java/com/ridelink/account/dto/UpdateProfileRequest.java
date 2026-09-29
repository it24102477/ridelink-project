package com.ridelink.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

public class UpdateProfileRequest {

    @Pattern(
    regexp = "^[\\p{L}. ]+$",
    message = "can only use letters for full name"
    )
     @Schema(example = "Nimal Perera")
    private String fullName;
    private String phoneNumber;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
