package com.ridelink.fare.service;

import com.ridelink.fare.client.RideDto;
import com.ridelink.fare.client.RideServiceClient;
import com.ridelink.fare.dto.CompleteRidePaymentRequest;
import com.ridelink.fare.dto.CreatePaymentRequest;
import com.ridelink.fare.dto.FinalFareRequest;
import com.ridelink.fare.dto.FinalFareResponse;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.exception.PaymentFailedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Drives the driver-facing "complete ride and pay" flow entirely from
 * fare-payment-service:
 *  1. Authenticate the caller against the ride (via ride-management-service - only the
 *     assigned driver, the passenger, or an ADMIN may look the ride up, so a 403 there
 *     means this driver does not own this ride).
 *  2. Calculate the final fare from the ride's real pickup/destination and the reported
 *     trip duration (this service's own responsibility, done locally - no HTTP hop).
 *  3. Record the payment here, in fare-payment-service, before anything else changes.
 *  4. Only once the payment exists, tell ride-management-service (localhost:8083) to
 *     mark the ride COMPLETED, which checks that this payment exists and takes the
 *     fare from it (no amount is ever accepted from a caller).
 */
@Service
public class RideCompletionService {

    private static final String IN_PROGRESS = "IN_PROGRESS";

    private final RideServiceClient rideServiceClient;
    private final FareService fareService;
    private final PaymentService paymentService;

    public RideCompletionService(RideServiceClient rideServiceClient, FareService fareService, PaymentService paymentService) {
        this.rideServiceClient = rideServiceClient;
        this.fareService = fareService;
        this.paymentService = paymentService;
    }

    public PaymentResponse completeRideAndPay(String rideId, String driverId, CompleteRidePaymentRequest req, String callerAuthorizationHeader,
                                              String authenticatedDriverUserId) {
        if (callerAuthorizationHeader == null || callerAuthorizationHeader.isBlank()) {
            throw new AccessDeniedException("Missing or empty bearer token");
        }

        // 1. Authenticate driverId + rideId together: the JWT proves who is calling (checked
        // by GET /api/rides/{id} below, which 403s unless the caller is this ride's assigned
        // driver), and this explicit check proves the driverId they claim in the URL really
        // is the driver ride-management-service has on record for this specific ride.
        RideDto ride = rideServiceClient.getRide(rideId, callerAuthorizationHeader);
        if (!driverId.equals(ride.getDriverId())) {
            throw new AccessDeniedException("driverId " + driverId
                    + " is not the driver assigned to ride " + rideId);
        }

        // 2. Payment (and completion) is only allowed once the ride has actually been started.
        if (!IN_PROGRESS.equals(ride.getStatus())) {
            throw new PaymentFailedException("ride " + rideId + " is " + ride.getStatus()
                    + ", not IN_PROGRESS - it must be started before it can be paid for and completed");
        }

        FinalFareRequest fareReq = new FinalFareRequest();
        fareReq.setRideId(rideId);
        fareReq.setPickupLat(ride.getPickup().getLat());
        fareReq.setPickupLng(ride.getPickup().getLng());
        fareReq.setDestinationLat(ride.getDestination().getLat());
        fareReq.setDestinationLng(ride.getDestination().getLng());
        fareReq.setActualDurationMinutes(req.getActualDurationMinutes());
        FinalFareResponse finalFare = fareService.finalFare(fareReq);

        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setRideId(rideId);
        payReq.setPassengerId(ride.getPassengerId());
        // The caller was already proven to be this ride's assigned driver (step 1), so their
        // account id is the driver who earned this payment.
        payReq.setDriverUserId(authenticatedDriverUserId);
        payReq.setAmount(finalFare.getFinalFare());
        payReq.setMethod(req.getPaymentMethod());
        PaymentResponse payment = paymentService.recordPayment(payReq);

        // The ride still happened and the payment record already exists even if this
        // downstream call fails - matching ride-management-service's own documented
        // "payment failure doesn't roll back completion" behaviour, just mirrored here.
        rideServiceClient.completeRide(rideId, callerAuthorizationHeader);

        return payment;
    }
}
