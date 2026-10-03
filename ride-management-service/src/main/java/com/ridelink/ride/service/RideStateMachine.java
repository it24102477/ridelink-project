package com.ridelink.ride.service;

import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.model.RideStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Documented ride lifecycle rules (report section: ride lifecycle / G2 evidence).
 *
 * A ride is created REQUESTED and open (unassigned) to every eligible driver in its
 * service area. Any of those drivers may attempt to accept it, but the accept path is
 * a single atomic "first write wins" claim (see RideService#accept) - so of however
 * many drivers race to accept the same ride, exactly one succeeds and moves the ride
 * to ACCEPTED; every other driver's attempt fails with a 409 conflict and the ride
 * stays as it was for them (already ACCEPTED by someone else, or already CANCELLED).
 *
 *   REQUESTED   -> ASSIGNED, ACCEPTED, CANCELLED
 *   ASSIGNED    -> ACCEPTED, CANCELLED
 *   ACCEPTED    -> IN_PROGRESS, CANCELLED
 *   IN_PROGRESS -> COMPLETED, CANCELLED
 *   COMPLETED   -> (terminal)
 *   CANCELLED   -> (terminal)
 */
@Component
public class RideStateMachine {

    private static final Map<RideStatus, Set<RideStatus>> ALLOWED = Map.of(
            RideStatus.REQUESTED, Set.of(RideStatus.ASSIGNED, RideStatus.ACCEPTED, RideStatus.CANCELLED),
            RideStatus.ASSIGNED, Set.of(RideStatus.ACCEPTED, RideStatus.CANCELLED),
            RideStatus.ACCEPTED, Set.of(RideStatus.IN_PROGRESS, RideStatus.CANCELLED),
            RideStatus.IN_PROGRESS, Set.of(RideStatus.COMPLETED, RideStatus.CANCELLED),
            RideStatus.COMPLETED, Set.of(),
            RideStatus.CANCELLED, Set.of()
    );

    public void assertTransitionAllowed(RideStatus from, RideStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
