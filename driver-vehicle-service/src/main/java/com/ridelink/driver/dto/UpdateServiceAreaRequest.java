package com.ridelink.driver.dto;

import com.ridelink.driver.validation.SriLankanDistrict;
import jakarta.validation.constraints.NotBlank;

public class UpdateServiceAreaRequest {
    @NotBlank(message = "Service area is required")
    @SriLankanDistrict
    private String serviceArea;

    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
}