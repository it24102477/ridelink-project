package com.ridelink.fare.dto;

import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;

import java.time.Instant;

public class PaymentResponse {
    private String id;
    private String rideId;
    private String passengerId;
    private String driverUserId;
    private double amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private String failureReason;
    private Instant createdAt;

    public static PaymentResponse from(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.id = p.getId();
        r.rideId = p.getRideId();
        r.passengerId = p.getPassengerId();
        r.driverUserId = p.getDriverUserId();
        r.amount = p.getAmount();
        r.currency = p.getCurrency();
        r.method = p.getMethod();
        r.status = p.getStatus();
        r.failureReason = p.getFailureReason();
        r.createdAt = p.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public String getRideId() { return rideId; }
    public String getPassengerId() { return passengerId; }
    public String getDriverUserId() { return driverUserId; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
