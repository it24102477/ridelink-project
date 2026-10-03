package com.ridelink.fare.repository;

import com.ridelink.fare.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByRideId(String rideId);
    List<Payment> findByPassengerId(String passengerId);
    List<Payment> findByDriverUserId(String driverUserId);
}
