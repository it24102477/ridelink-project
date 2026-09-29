package com.ridelink.driver.dto;

import com.ridelink.driver.validation.SriLankanDistrict;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateDriverRequest {

    @NotBlank(message = "License number is required")
    @Pattern(regexp = "^[A-Z][0-9]{7}$",
             message = "License number must start with a capital English letter followed by 7 digits (e.g. B1234567)")
    private String licenseNumber;

    @NotNull(message = "Vehicle is required") @Valid
    private VehicleDto vehicle;

    @NotBlank(message = "Service area is required")
    @SriLankanDistrict
    private String serviceArea;

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public VehicleDto getVehicle() { return vehicle; }
    public void setVehicle(VehicleDto vehicle) { this.vehicle = vehicle; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
}