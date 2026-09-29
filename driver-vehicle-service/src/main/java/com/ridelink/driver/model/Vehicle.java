package com.ridelink.driver.model;

import org.springframework.data.mongodb.core.index.Indexed;

public class Vehicle {
    private String make;
    private String model;

    @Indexed(unique = true)
    private String plateNumber;

    private String color;
    private int capacity;

    public Vehicle() {}

    public Vehicle(String make, String model, String plateNumber, String color, int capacity) {
        this.make = make;
        this.model = model;
        this.plateNumber = plateNumber;
        this.color = color;
        this.capacity = capacity;
    }

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
