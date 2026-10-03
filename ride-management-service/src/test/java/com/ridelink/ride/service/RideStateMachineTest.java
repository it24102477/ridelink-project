package com.ridelink.ride.service;

import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.model.RideStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideStateMachineTest {

    private final RideStateMachine machine = new RideStateMachine();

    @Test
    void allowsDocumentedHappyPathTransitions() {
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.REQUESTED, RideStatus.ASSIGNED));
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.ASSIGNED, RideStatus.ACCEPTED));
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS));
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.IN_PROGRESS, RideStatus.COMPLETED));
    }

    @Test
    void rejectsSkippingStates() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> machine.assertTransitionAllowed(RideStatus.REQUESTED, RideStatus.IN_PROGRESS));
    }

    @Test
    void rejectsTransitionsOutOfTerminalStates() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> machine.assertTransitionAllowed(RideStatus.COMPLETED, RideStatus.CANCELLED));
        assertThrows(InvalidStatusTransitionException.class,
                () -> machine.assertTransitionAllowed(RideStatus.CANCELLED, RideStatus.ASSIGNED));
    }

    @Test
    void allowsCancellationFromAnyNonTerminalState() {
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.REQUESTED, RideStatus.CANCELLED));
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.ASSIGNED, RideStatus.CANCELLED));
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.ACCEPTED, RideStatus.CANCELLED));
        assertDoesNotThrow(() -> machine.assertTransitionAllowed(RideStatus.IN_PROGRESS, RideStatus.CANCELLED));
    }
}
