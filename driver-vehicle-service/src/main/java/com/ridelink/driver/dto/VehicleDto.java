package com.ridelink.driver.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VehicleDto {
    @NotBlank(message = "Vehicle make is required") private String make;
    @NotBlank(message = "Vehicle model is required") private String model;

    @NotBlank(message = "Plate number is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]+$",
         message = "Plate number must contain both English letters and numbers, and no other characters (e.g. CAB1234)")
private String plateNumber;

    private String color;
    @Min(value = 1, message = "Capacity must be at least 1") private int capacity = 4;

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}