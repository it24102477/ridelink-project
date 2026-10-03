package com.ridelink.ride.repository;

import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface RideRepository extends MongoRepository<Ride, String> {
    List<Ride> findByPassengerId(String passengerId);
    List<Ride> findByDriverId(String driverId);
    /** Rides of the driver whose account-service User id is {@code driverUserId}. */
    List<Ride> findByDriverUserId(String driverUserId);
    long countByPassengerIdAndStatusIn(String passengerId, Collection<RideStatus> statuses);
    long countByDriverUserIdAndStatusIn(String driverUserId, Collection<RideStatus> statuses);
}
