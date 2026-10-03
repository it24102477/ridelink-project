package com.ridelink.fare.dto;

import com.ridelink.fare.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreatePaymentRequest {
    @NotBlank private String rideId;
    @NotBlank private String passengerId;
    /** Optional: the driver's account id, so the driver can later list their own payments. */
    private String driverUserId;
    @Positive private double amount;
    @NotNull private PaymentMethod method;

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getDriverUserId() { return driverUserId; }
    public void setDriverUserId(String driverUserId) { this.driverUserId = driverUserId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
}
