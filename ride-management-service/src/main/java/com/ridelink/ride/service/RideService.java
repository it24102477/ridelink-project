package com.ridelink.ride.service;

import com.ridelink.ride.client.*;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.DriverNotEligibleException;
import com.ridelink.ride.exception.RideAlreadyAcceptedException;
import com.ridelink.ride.exception.RideNotCompletedException;
import com.ridelink.ride.exception.ServiceAreaNotFoundException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    /** Statuses that mean the ride is still in play (not COMPLETED / CANCELLED). */
    private static final Set<RideStatus> OPEN_STATUSES = EnumSet.of(
            RideStatus.REQUESTED, RideStatus.ASSIGNED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS);

    private final RideRepository rideRepository;
    private final MongoTemplate mongoTemplate;
    private final RideStateMachine stateMachine;
    private final DriverServiceClient driverServiceClient;
    private final FareServiceClient fareServiceClient;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final GeocodingClient geocodingClient;

    public RideService(RideRepository rideRepository,
                        MongoTemplate mongoTemplate,
                        RideStateMachine stateMachine,
                        DriverServiceClient driverServiceClient,
                        FareServiceClient fareServiceClient,
                        SequenceGeneratorService sequenceGeneratorService,
                        GeocodingClient geocodingClient) {
        this.rideRepository = rideRepository;
        this.mongoTemplate = mongoTemplate;
        this.stateMachine = stateMachine;
        this.driverServiceClient = driverServiceClient;
        this.fareServiceClient = fareServiceClient;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.geocodingClient = geocodingClient;
    }

    /**
     * Workflow 4 (ride request and broadcast):
     * 1. Get a fare estimate from fare-payment-service.
     * 2. Create the ride in REQUESTED state with no driver attached. It is not pushed
     *    to or reserved for any single driver - every eligible driver in the service
     *    area can see it (via GET /api/drivers/available-style polling, or a future
     *    push notification) and race to accept it. See {@link #accept} for how exactly
     *    one of them is allowed to win that race.
     */
    /** Address(es) in, resolved coordinates + road distance + estimated fare out. Saves nothing. */
    public EstimateRideResponse estimate(EstimateRideRequest req) {
        Location pickup = resolve(req.getPickup());
        Location destination = resolve(req.getDestination());
        FareEstimateDto e = fareServiceClient.getEstimate(pickup, destination);
        return new EstimateRideResponse(pickup, destination, e.getDistanceKm(), e.getEstimatedFare(), e.getCurrency());
    }

    public RideResponse createRide(String userId, CreateRideRequest req, String authenticatedUserId) {
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("You can only request a ride for your own account");
        }
        Location pickup = resolve(req.getPickup());
        Location destination = resolve(req.getDestination());

        // The service area is the pickup's district - not something the passenger types in.
        String serviceArea = pickup.getDistrict();
        if (serviceArea == null) {
            throw new ServiceAreaNotFoundException(req.getPickup());
        }

        FareEstimateDto estimate = fareServiceClient.getEstimate(pickup, destination);

        Ride ride = new Ride(userId, pickup, destination, serviceArea);
        ride.setId(sequenceGeneratorService.nextId("rides"));
        ride.setEstimatedFare(estimate.getEstimatedFare());
        ride.setDistanceKm(estimate.getDistanceKm());
        ride.setStatus(RideStatus.REQUESTED);

        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Any driver eligible for this ride (their own profile, currently AVAILABLE, in the
     * ride's service area) may call this. Because many drivers can attempt it for the
     * same ride at the same time, ownership is decided by a single atomic
     * find-and-modify against the database - not by a read-then-write in application
     * code, which would be racy. The update only matches a document that is still
     * REQUESTED and has no driver on it yet; whichever request's findAndModify lands
     * first flips both conditions, so every later request - however close behind -
     * simply fails to match and gets a 409, instead of silently overwriting the winner.
     */
    public RideResponse accept(String rideId, String driverId, String authenticatedUserId, String callerAuthorizationHeader) {
        Ride ride = findOrThrow(rideId);
        stateMachine.assertTransitionAllowed(ride.getStatus(), RideStatus.ACCEPTED);

        DriverProfileDto driver = driverServiceClient.getDriver(driverId, callerAuthorizationHeader);
        if (driver == null || !authenticatedUserId.equals(driver.getUserId())) {
            throw new AccessDeniedException("You can only accept a ride using your own driver profile");
        }
        // The ride must be in the driver's own service area - checked first, so a driver from another
        // area gets the service-area message rather than an availability one.
        if (ride.getServiceArea() != null && !ride.getServiceArea().equalsIgnoreCase(driver.getServiceArea())) {
            throw new DriverNotEligibleException("This ride is not in your service area");
        }
        if (!"AVAILABLE".equals(driver.getAvailability())) {
            throw new DriverNotEligibleException("You are not currently AVAILABLE, so you cannot accept a ride");
        }

        Query claimQuery = Query.query(Criteria.where("id").is(rideId)
                .and("status").is(RideStatus.REQUESTED)
                .and("driverId").isNull());
        Update claimUpdate = new Update()
                .set("driverId", driverId)
                .set("driverUserId", authenticatedUserId)
                .set("status", RideStatus.ACCEPTED)
                .set("acceptedAt", Instant.now());

        Ride claimed = mongoTemplate.findAndModify(claimQuery, claimUpdate,
                FindAndModifyOptions.options().returnNew(true), Ride.class);

        if (claimed == null) {
            // Someone else's accept won the race first (or the ride moved on/was
            // cancelled between our read above and this write).
            throw new RideAlreadyAcceptedException(rideId);
        }
        // The driver now has a ride, so mark them BUSY in driver-vehicle-service. If that call
        // fails, hand the ride back to the pool instead of leaving an ACCEPTED ride whose driver
        // still shows as AVAILABLE (and could win a second ride).
        try {
            driverServiceClient.markBusy(driverId, authenticatedUserId, callerAuthorizationHeader);
        } catch (RuntimeException ex) {
            Query rollbackQuery = Query.query(Criteria.where("id").is(rideId)
                    .and("driverId").is(driverId)
                    .and("status").is(RideStatus.ACCEPTED));
            Update rollback = new Update()
                    .set("status", RideStatus.REQUESTED)
                    .unset("driverId")
                    .unset("driverUserId")
                    .unset("acceptedAt");
            mongoTemplate.updateFirst(rollbackQuery, rollback, Ride.class);
            throw ex;
        }
        return RideResponse.from(claimed);
    }

    public RideResponse start(String rideId, String authenticatedUserId) {
        Ride ride = findOrThrow(rideId);
        requireAssignedDriver(ride, authenticatedUserId);
        stateMachine.assertTransitionAllowed(ride.getStatus(), RideStatus.IN_PROGRESS);
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Workflow 6 (completion). A ride can only be completed AFTER it has been paid through the
     * payment API (POST fare-payment-service /api/payments/rides/{rideId}/{driverId}/Payment). No amount is
     * accepted from the caller: this method looks up the driver's COMPLETED payment for the ride
     * in fare-payment-service and uses that payment's amount as the final fare. A direct call
     * without a payment is rejected.
     */
    public RideResponse complete(String rideId, String authenticatedUserId, String callerAuthorizationHeader) {
        Ride ride = findOrThrow(rideId);
        requireAssignedDriver(ride, authenticatedUserId);
        stateMachine.assertTransitionAllowed(ride.getStatus(), RideStatus.COMPLETED);

        PaymentResultDto payment = fareServiceClient.findDriverPaymentForRide(
                authenticatedUserId, ride.getId(), callerAuthorizationHeader);
        if (payment == null || !"COMPLETED".equals(payment.getStatus())) {
            throw new AccessDeniedException("A ride can not be completed directly. Complete it through the payment API: "
                    + "POST http://localhost:8084/api/payments/rides/" + rideId + "/" + ride.getDriverId() + "/Payment");
        }

        ride.setFinalFare(payment.getAmount());
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());
        Ride saved = rideRepository.save(ride);

        // The driver is free again now that the ride is done. Best-effort: a failure here does not
        // undo the completed ride or its payment - it just leaves the driver as BUSY until they
        // update their own availability.
        try {
            driverServiceClient.markAvailable(ride.getDriverId(), ride.getDriverUserId(), callerAuthorizationHeader);
        } catch (RuntimeException ex) {
            log.warn("Could not mark driver {} AVAILABLE after completing ride {}: {}",
                    ride.getDriverId(), ride.getId(), ex.getMessage());
        }

        return RideResponse.from(saved);
    }

    /** Cancel by one of the ride's own participants (its passenger or its assigned driver). */
    public RideResponse cancel(String rideId, CancelRideRequest req, String authenticatedUserId) {
        Ride ride = findOrThrow(rideId);
        boolean isPassenger = ride.getPassengerId().equals(authenticatedUserId);
        boolean isAssignedDriver = authenticatedUserId.equals(ride.getDriverUserId());
        if (!isPassenger && !isAssignedDriver) {
            throw new AccessDeniedException("You can only cancel your own ride");
        }
        return applyCancellation(ride, req.getReason());
    }

    /** Passenger endpoint: the {userId} in the URL must be the caller. */
    public RideResponse cancelForPassenger(String pathUserId, String rideId, CancelRideRequest req,
                                           String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        return cancel(rideId, req, authenticatedUserId);
    }

    /**
     * ADMIN only. Moves a ride that is REQUESTED, ASSIGNED, ACCEPTED or IN_PROGRESS to CANCELLED.
     * A COMPLETED or CANCELLED ride is rejected by the state machine.
     */
    public RideResponse cancelAsAdmin(String rideId, CancelRideRequest req) {
        return applyCancellation(findOrThrow(rideId), req.getReason());
    }

    private RideResponse applyCancellation(Ride ride, String reason) {
        stateMachine.assertTransitionAllowed(ride.getStatus(), RideStatus.CANCELLED);
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(reason);
        ride.setCancelledAt(Instant.now());
        return RideResponse.from(rideRepository.save(ride));
    }

    /** ADMIN only. A ride can be deleted only once it is COMPLETED or CANCELLED. */
    public void deleteRide(String rideId) {
        Ride ride = findOrThrow(rideId);
        if (ride.getStatus() != RideStatus.COMPLETED && ride.getStatus() != RideStatus.CANCELLED) {
            throw new RideNotCompletedException(ride.getStatus());
        }
        rideRepository.deleteById(rideId);
    }

    public RideResponse getById(String id, String authenticatedUserId, String authenticatedRole) {
        Ride ride = findOrThrow(id);
        requireParticipantOrAdmin(ride, authenticatedUserId, authenticatedRole);
        return RideResponse.from(ride);
    }

    /** ADMIN only (enforced in SecurityConfig): every ride. */
    public List<RideResponse> listAll() {
        return rideRepository.findAll().stream().map(RideResponse::from).collect(Collectors.toList());
    }

    /** PASSENGER: only the caller's own rides. */
    public List<RideResponse> getPassengerRides(String pathUserId, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        return rideRepository.findByPassengerId(pathUserId).stream()
                .map(RideResponse::from).collect(Collectors.toList());
    }

    /** DRIVER: only the rides the caller has accepted (matched on the caller's account id). */
    public List<RideResponse> getDriverRides(String pathUserId, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        return rideRepository.findByDriverUserId(pathUserId).stream()
                .map(RideResponse::from).collect(Collectors.toList());
    }

    /**
     * Does this user (as passenger or as driver) still have a ride that is not COMPLETED or
     * CANCELLED? Callable by the user themself or an ADMIN - account-service and
     * driver-vehicle-service use it before deleting an account / driver profile.
     */
    public OpenRidesResponse getOpenRides(String userId, String authenticatedUserId, String authenticatedRole) {
        if (!"ADMIN".equals(authenticatedRole) && !userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only check your own rides");
        }
        long open = rideRepository.countByPassengerIdAndStatusIn(userId, OPEN_STATUSES)
                + rideRepository.countByDriverUserIdAndStatusIn(userId, OPEN_STATUSES);
        return new OpenRidesResponse(userId, open);
    }

    /** Address -> coordinates using the free OSM geocoder (Nominatim). */
    private Location resolve(String address) {
        return geocodingClient.geocode(address);
    }

    /** The {userId} in the URL must be the authenticated caller's own id. */
    private void requireSelf(String pathUserId, String authenticatedUserId) {
        if (!pathUserId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only access your own rides");
        }
    }

    private void requireAssignedDriver(Ride ride, String authenticatedUserId) {
        if (!authenticatedUserId.equals(ride.getDriverUserId())) {
            throw new AccessDeniedException("You are not the driver assigned to this ride");
        }
    }

    private void requireParticipantOrAdmin(Ride ride, String authenticatedUserId, String authenticatedRole) {
        boolean isAdmin = "ADMIN".equals(authenticatedRole);
        boolean isPassenger = ride.getPassengerId().equals(authenticatedUserId);
        boolean isAssignedDriver = authenticatedUserId.equals(ride.getDriverUserId());
        if (!isAdmin && !isPassenger && !isAssignedDriver) {
            throw new AccessDeniedException("You are not a participant in this ride");
        }
    }

    private Ride findOrThrow(String id) {
        return rideRepository.findById(id).orElseThrow(() -> new RideNotFoundException(id));
    }
}
