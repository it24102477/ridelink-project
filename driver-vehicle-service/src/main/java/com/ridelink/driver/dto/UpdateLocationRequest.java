package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The driver types an address; latitude and longitude are worked out by the service. */
public class UpdateLocationRequest {

    @NotBlank(message = "Address is required")
    @Size(max = 200, message = "Address must be at most 200 characters")
    private String address;

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
