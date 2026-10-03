package com.ridelink.ride.service;

import com.ridelink.ride.client.*;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.exception.DriverNotEligibleException;
import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.exception.RideAlreadyAcceptedException;
import com.ridelink.ride.exception.RideNotCompletedException;
import com.ridelink.ride.exception.ServiceAreaNotFoundException;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private DriverServiceClient driverServiceClient;
    @Mock private FareServiceClient fareServiceClient;
    @Mock private SequenceGeneratorService sequenceGeneratorService;
    @Mock private GeocodingClient geocodingClient;
    private RideService rideService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rideService = new RideService(rideRepository, mongoTemplate, new RideStateMachine(), driverServiceClient,
                fareServiceClient, sequenceGeneratorService, geocodingClient);
        lenient().when(sequenceGeneratorService.nextId(any())).thenReturn("1");
        lenient().when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateRideRequest sampleRequest() {
        CreateRideRequest req = new CreateRideRequest();
        req.setPickup("Colombo Fort");
        req.setDestination("Kandy");
        return req;
    }

    private void stubEstimate() {
        Location pickup = new Location(6.9271, 79.8612, "Colombo Fort");
        pickup.setDistrict("Colombo");
        lenient().when(geocodingClient.geocode("Colombo Fort")).thenReturn(pickup);
        lenient().when(geocodingClient.geocode("Kandy")).thenReturn(new Location(7.2906, 80.6337, "Kandy"));
        FareEstimateDto estimate = new FareEstimateDto();
        estimate.setEstimatedFare(1200.0);
        estimate.setDistanceKm(45.0);
        when(fareServiceClient.getEstimate(any(), any())).thenReturn(estimate);
    }

    @Test
    void createRide_createsOpenRequestedRide_withNoDriverAttached() {
        stubEstimate();

        var response = rideService.createRide("passenger-1", sampleRequest(), "passenger-1");

        assertEquals(RideStatus.REQUESTED, response.getStatus());
        assertNull(response.getDriverId());
        assertEquals(1200.0, response.getEstimatedFare());
        verifyNoInteractions(driverServiceClient);
    }

    @Test
    void createRide_setsServiceAreaFromThePickupsResolvedDistrict() {
        stubEstimate();
        var response = rideService.createRide("passenger-1", sampleRequest(), "passenger-1");
        assertEquals("Colombo", response.getServiceArea());
    }

    @Test
    void createRide_throwsServiceAreaNotFound_whenThePickupDistrictCannotBeResolved() {
        Location pickup = new Location(6.9271, 79.8612, "Somewhere unrecognisable");
        lenient().when(geocodingClient.geocode("Colombo Fort")).thenReturn(pickup);
        lenient().when(geocodingClient.geocode("Kandy")).thenReturn(new Location(7.2906, 80.6337, "Kandy"));

        assertThrows(ServiceAreaNotFoundException.class,
                () -> rideService.createRide("passenger-1", sampleRequest(), "passenger-1"));
        verifyNoInteractions(fareServiceClient);
    }

    @Test
    void createRide_throwsAccessDenied_whenUserIdInPathDoesNotMatchCaller() {
        assertThrows(AccessDeniedException.class, () -> rideService.createRide("passenger-1", sampleRequest(), "someone-else"));
        verifyNoInteractions(fareServiceClient, driverServiceClient);
    }

    private Ride openRide() {
        Ride ride = new Ride("passenger-1", new Location(6.9271, 79.8612, "Colombo Fort"),
                new Location(7.2906, 80.6337, "Kandy"), "Colombo");
        ride.setId("ride-1");
        ride.setStatus(RideStatus.REQUESTED);
        return ride;
    }

    private Ride acceptedRide() {
        Ride ride = openRide();
        ride.setDriverId("driver-1");
        ride.setDriverUserId("driver-user-1");
        ride.setStatus(RideStatus.ACCEPTED);
        return ride;
    }

    private DriverProfileDto eligibleDriverProfile() {
        DriverProfileDto driver = new DriverProfileDto();
        driver.setId("driver-1");
        driver.setUserId("driver-user-1");
        driver.setServiceArea("Colombo");
        driver.setAvailability("AVAILABLE");
        return driver;
    }

    @Test
    void accept_claimsOpenRide_forAnyEligibleDriver() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide()));
        when(driverServiceClient.getDriver("driver-1", "Bearer token")).thenReturn(eligibleDriverProfile());
        Ride claimed = acceptedRide();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(claimed);

        var response = rideService.accept("ride-1", "driver-1", "driver-user-1", "Bearer token");

        assertEquals(RideStatus.ACCEPTED, response.getStatus());
        assertEquals("driver-1", response.getDriverId());
    }

    @Test
    void accept_throwsRideAlreadyAccepted_whenAnotherDriverWonTheRace() {
        // Two drivers race for the same open ride; the atomic claim reports "no match"
        // for whichever request arrives second, however close behind.
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide()));
        when(driverServiceClient.getDriver("driver-2", "Bearer token")).thenReturn(eligibleDriverProfileWith("driver-2", "driver-user-2"));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(null);

        assertThrows(RideAlreadyAcceptedException.class, () -> rideService.accept("ride-1", "driver-2", "driver-user-2", "Bearer token"));
    }

    private DriverProfileDto eligibleDriverProfileWith(String driverId, String userId) {
        DriverProfileDto driver = new DriverProfileDto();
        driver.setId(driverId);
        driver.setUserId(userId);
        driver.setServiceArea("Colombo");
        driver.setAvailability("AVAILABLE");
        return driver;
    }

    @Test
    void accept_throwsAccessDenied_whenDriverProfileBelongsToSomeoneElse() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide()));
        DriverProfileDto driver = eligibleDriverProfile();
        driver.setUserId("someone-else");
        when(driverServiceClient.getDriver("driver-1", "Bearer token")).thenReturn(driver);

        assertThrows(AccessDeniedException.class, () -> rideService.accept("ride-1", "driver-1", "driver-user-1", "Bearer token"));
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void accept_throwsDriverNotEligible_withServiceAreaMessage_whenDriverIsInAnotherServiceArea() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide())); // ride in Colombo
        DriverProfileDto driver = eligibleDriverProfile();
        driver.setServiceArea("Kandy");
        when(driverServiceClient.getDriver("driver-1", "Bearer token")).thenReturn(driver);

        DriverNotEligibleException ex = assertThrows(DriverNotEligibleException.class,
                () -> rideService.accept("ride-1", "driver-1", "driver-user-1", "Bearer token"));
        assertEquals("This ride is not in your service area", ex.getMessage());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void accept_throwsDriverNotEligible_whenDriverIsNotAvailable() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide()));
        DriverProfileDto driver = eligibleDriverProfile();
        driver.setAvailability("BUSY");
        when(driverServiceClient.getDriver("driver-1", "Bearer token")).thenReturn(driver);

        assertThrows(DriverNotEligibleException.class, () -> rideService.accept("ride-1", "driver-1", "driver-user-1", "Bearer token"));
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void accept_throwsInvalidStatusTransition_whenRideIsNoLongerOpen() {
        Ride ride = acceptedRide(); // already claimed by another driver earlier
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertThrows(RuntimeException.class, () -> rideService.accept("ride-1", "driver-2", "driver-user-2", "Bearer token"));
        verifyNoInteractions(mongoTemplate);
    }

    private PaymentResultDto payment(String status, double amount) {
        PaymentResultDto p = new PaymentResultDto();
        p.setStatus(status);
        p.setAmount(amount);
        return p;
    }

    @Test
    void complete_usesTheAmountOfTheExistingPayment_forTheAcceptedDriver() {
        Ride ride = acceptedRide();
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
        when(fareServiceClient.findDriverPaymentForRide("driver-user-1", "ride-1", "Bearer token"))
                .thenReturn(payment("COMPLETED", 1500.0));

        var response = rideService.complete("ride-1", "driver-user-1", "Bearer token");

        assertEquals(RideStatus.COMPLETED, response.getStatus());
        assertEquals(1500.0, response.getFinalFare());
        verify(driverServiceClient).markAvailable("driver-1", "driver-user-1", "Bearer token");
    }

    @Test
    void complete_isRejected_whenTheRideHasNotBeenPaidThroughThePaymentApi() {
        Ride ride = acceptedRide();
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
        when(fareServiceClient.findDriverPaymentForRide("driver-user-1", "ride-1", "Bearer token")).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> rideService.complete("ride-1", "driver-user-1", "Bearer token"));
        verify(rideRepository, never()).save(any());
    }

    @Test
    void complete_stillCompletes_whenMarkingTheDriverAvailableFails() {
        Ride ride = acceptedRide();
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
        when(fareServiceClient.findDriverPaymentForRide(any(), any(), any())).thenReturn(payment("COMPLETED", 1500.0));
        doThrow(new DownstreamServiceException("driver-vehicle-service", null))
                .when(driverServiceClient).markAvailable(any(), any(), any());

        var response = rideService.complete("ride-1", "driver-user-1", "Bearer token");

        assertEquals(RideStatus.COMPLETED, response.getStatus());
    }

    @Test
    void complete_throwsAccessDenied_forADriverNotAssignedToTheRide() {
        Ride ride = acceptedRide();
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertThrows(AccessDeniedException.class, () -> rideService.complete("ride-1", "some-other-driver", "Bearer token"));
        verifyNoInteractions(fareServiceClient);
    }

    @Test
    void cancel_succeeds_forTheRequestingPassenger() {
        Ride ride = acceptedRide();
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        CancelRideRequest req = new CancelRideRequest();
        req.setReason("Change of plans");

        var response = rideService.cancel("ride-1", req, "passenger-1");

        assertEquals(RideStatus.CANCELLED, response.getStatus());
    }

    @Test
    void cancel_succeeds_forTheAcceptedDriver() {
        Ride ride = acceptedRide();
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        CancelRideRequest req = new CancelRideRequest();
        req.setReason("Vehicle issue");

        var response = rideService.cancel("ride-1", req, "driver-user-1");

        assertEquals(RideStatus.CANCELLED, response.getStatus());
    }

    @Test
    void cancel_throwsAccessDenied_forAnUnrelatedUser() {
        Ride ride = acceptedRide();
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        CancelRideRequest req = new CancelRideRequest();
        req.setReason("Not my ride");

        assertThrows(AccessDeniedException.class, () -> rideService.cancel("ride-1", req, "random-user"));
        verify(rideRepository, never()).save(any());
    }

    @Test
    void getById_throwsAccessDenied_forANonParticipant() {
        Ride ride = acceptedRide();
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertThrows(AccessDeniedException.class, () -> rideService.getById("ride-1", "random-user", "PASSENGER"));
    }

    @Test
    void getById_succeeds_forTheParticipatingPassenger() {
        Ride ride = acceptedRide();
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertDoesNotThrow(() -> rideService.getById("ride-1", "passenger-1", "PASSENGER"));
    }

    @Test
    void getById_succeeds_forAdmin_regardlessOfParticipation() {
        Ride ride = acceptedRide();
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        assertDoesNotThrow(() -> rideService.getById("ride-1", "admin-1", "ADMIN"));
    }

    // ---------------------------------------------------------------- own ride lists

    @Test
    void getPassengerRides_throwsAccessDenied_whenQueryingSomeoneElsesRides() {
        assertThrows(AccessDeniedException.class,
                () -> rideService.getPassengerRides("passenger-1", "someone-else"));
        verify(rideRepository, never()).findByPassengerId(any());
    }

    @Test
    void getPassengerRides_returnsOnlyTheCallersRides() {
        when(rideRepository.findByPassengerId("passenger-1")).thenReturn(java.util.List.of(openRide()));
        assertEquals(1, rideService.getPassengerRides("passenger-1", "passenger-1").size());
    }

    @Test
    void getDriverRides_throwsAccessDenied_whenQueryingSomeoneElsesRides() {
        assertThrows(AccessDeniedException.class,
                () -> rideService.getDriverRides("driver-user-1", "someone-else"));
        verify(rideRepository, never()).findByDriverUserId(any());
    }

    @Test
    void getDriverRides_returnsRidesByTheCallersAccountId() {
        when(rideRepository.findByDriverUserId("driver-user-1")).thenReturn(java.util.List.of(acceptedRide()));
        assertEquals(1, rideService.getDriverRides("driver-user-1", "driver-user-1").size());
    }

    @Test
    void listAll_returnsEveryRide() {
        when(rideRepository.findAll()).thenReturn(java.util.List.of(openRide(), acceptedRide()));
        assertEquals(2, rideService.listAll().size());
    }

    // ---------------------------------------------------------------- driver becomes BUSY on accept

    @Test
    void accept_marksTheWinningDriverBusy() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide()));
        when(driverServiceClient.getDriver("driver-1", "Bearer token")).thenReturn(eligibleDriverProfile());
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(acceptedRide());

        rideService.accept("ride-1", "driver-1", "driver-user-1", "Bearer token");

        verify(driverServiceClient).markBusy("driver-1", "driver-user-1", "Bearer token");
    }

    @Test
    void accept_doesNotMarkBusy_whenAnotherDriverWonTheRace() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide()));
        when(driverServiceClient.getDriver("driver-2", "Bearer token"))
                .thenReturn(eligibleDriverProfileWith("driver-2", "driver-user-2"));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(null);

        assertThrows(RideAlreadyAcceptedException.class,
                () -> rideService.accept("ride-1", "driver-2", "driver-user-2", "Bearer token"));
        verify(driverServiceClient, never()).markBusy(any(), any(), any());
    }

    @Test
    void accept_releasesTheRide_whenMarkingTheDriverBusyFails() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(openRide()));
        when(driverServiceClient.getDriver("driver-1", "Bearer token")).thenReturn(eligibleDriverProfile());
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(acceptedRide());
        doThrow(new DownstreamServiceException("driver-vehicle-service", null))
                .when(driverServiceClient).markBusy(any(), any(), any());

        assertThrows(DownstreamServiceException.class,
                () -> rideService.accept("ride-1", "driver-1", "driver-user-1", "Bearer token"));
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Ride.class));
    }

    // ---------------------------------------------------------------- admin cancel

    @Test
    void cancelAsAdmin_cancelsRides_inEveryOpenStatus() {
        for (RideStatus status : new RideStatus[]{RideStatus.REQUESTED, RideStatus.ASSIGNED,
                RideStatus.ACCEPTED, RideStatus.IN_PROGRESS}) {
            Ride ride = acceptedRide();
            ride.setStatus(status);
            when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
            CancelRideRequest req = new CancelRideRequest();
            req.setReason("Admin decision");

            var response = rideService.cancelAsAdmin("ride-1", req);

            assertEquals(RideStatus.CANCELLED, response.getStatus(), "from " + status);
        }
    }

    @Test
    void cancelAsAdmin_rejectsCompletedAndCancelledRides() {
        for (RideStatus status : new RideStatus[]{RideStatus.COMPLETED, RideStatus.CANCELLED}) {
            Ride ride = acceptedRide();
            ride.setStatus(status);
            when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
            CancelRideRequest req = new CancelRideRequest();
            req.setReason("Admin decision");

            assertThrows(InvalidStatusTransitionException.class, () -> rideService.cancelAsAdmin("ride-1", req));
        }
    }

    // ---------------------------------------------------------------- admin delete

    @Test
    void deleteRide_succeeds_forCompletedAndCancelledRides() {
        for (RideStatus status : new RideStatus[]{RideStatus.COMPLETED, RideStatus.CANCELLED}) {
            Ride ride = acceptedRide();
            ride.setStatus(status);
            when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

            rideService.deleteRide("ride-1");
        }
        verify(rideRepository, times(2)).deleteById("ride-1");
    }

    @Test
    void deleteRide_rejectsRidesThatAreNotCompleted() {
        for (RideStatus status : new RideStatus[]{RideStatus.REQUESTED, RideStatus.ASSIGNED,
                RideStatus.ACCEPTED, RideStatus.IN_PROGRESS}) {
            Ride ride = acceptedRide();
            ride.setStatus(status);
            when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

            var ex = assertThrows(RideNotCompletedException.class, () -> rideService.deleteRide("ride-1"));
            assertTrue(ex.getMessage().contains("Ride not completed"));
        }
        verify(rideRepository, never()).deleteById(any());
    }

    // ---------------------------------------------------------------- open rides (used before account/profile deletion)

    @Test
    void getOpenRides_countsRidesAsPassengerAndAsDriver() {
        when(rideRepository.countByPassengerIdAndStatusIn(eq("user-1"), any())).thenReturn(1L);
        when(rideRepository.countByDriverUserIdAndStatusIn(eq("user-1"), any())).thenReturn(2L);

        var result = rideService.getOpenRides("user-1", "user-1", "PASSENGER");

        assertEquals(3, result.getOpenRideCount());
        assertTrue(result.isHasOpenRides());
    }

    @Test
    void getOpenRides_reportsNone_whenEveryRideIsFinished() {
        var result = rideService.getOpenRides("user-1", "admin-1", "ADMIN");
        assertEquals(0, result.getOpenRideCount());
        assertFalse(result.isHasOpenRides());
    }

    @Test
    void getOpenRides_throwsAccessDenied_forSomeoneElsesAccount() {
        assertThrows(AccessDeniedException.class, () -> rideService.getOpenRides("user-1", "user-2", "PASSENGER"));
    }

    @Test
    void cancelForPassenger_throwsAccessDenied_whenPathUserIdIsNotTheCaller() {
        CancelRideRequest req = new CancelRideRequest();
        req.setReason("x");
        assertThrows(AccessDeniedException.class,
                () -> rideService.cancelForPassenger("passenger-1", "ride-1", req, "someone-else"));
    }
}
