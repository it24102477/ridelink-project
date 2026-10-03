package com.ridelink.fare.dto;

import com.ridelink.fare.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Body for the driver-facing "create payment, then complete the ride" call. rideId comes
 * and driverId both come from the path; driverId is checked against the driverId
 * ride-management-service has on record for that ride (in addition to the JWT-based
 * ownership check performed via the GET /api/rides/{id} call itself). */
public class CompleteRidePaymentRequest {
    @Positive(message = "actualDurationMinutes can not be zero (it must be greater than zero)")
    private double actualDurationMinutes;
    @NotNull private PaymentMethod paymentMethod;

    public double getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(double actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
