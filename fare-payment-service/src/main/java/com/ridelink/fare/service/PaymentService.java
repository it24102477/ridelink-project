package com.ridelink.fare.service;

import com.ridelink.fare.dto.CreatePaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.exception.PaymentNotFoundException;
import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final String currency;
    private final SequenceGeneratorService sequenceGeneratorService;

    public PaymentService(PaymentRepository paymentRepository, @Value("${ridelink.fare.currency}") String currency,
                           SequenceGeneratorService sequenceGeneratorService) {
        this.paymentRepository = paymentRepository;
        this.currency = currency;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    /**
     * Records a simulated payment. Documented simulated-failure rule (for negative-scenario
     * demonstration, requirement #7 in the brief): a payment amount of exactly 0 or a negative
     * amount is rejected by request validation; beyond that, any request whose amount does not
     * match a sane positive value in Rs. 50 - Rs. 50,000 range is treated as a simulated gateway
     * failure so both success and failure paths can be demonstrated without a real payment gateway.
     */
    public PaymentResponse recordPayment(CreatePaymentRequest req) {
        Payment payment = new Payment(req.getRideId(), req.getPassengerId(), req.getAmount(), currency, req.getMethod());
        payment.setDriverUserId(req.getDriverUserId());
        payment.setId(sequenceGeneratorService.nextId("payments"));

        if (req.getAmount() < 50 || req.getAmount() > 50_000) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Simulated gateway declined amount outside allowed range (Rs.50 - Rs.50,000)");
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
        }

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    // ------------------------------------------------------------------ shared (User controller)

    /** A payment may be read by its passenger, by the driver who was paid, or by an ADMIN. */
    public PaymentResponse getById(String id, String authenticatedUserId, String authenticatedRole) {
        Payment payment = findOrThrow(id);
        boolean isAdmin = "ADMIN".equals(authenticatedRole);
        boolean isPassenger = payment.getPassengerId().equals(authenticatedUserId);
        boolean isDriver = authenticatedUserId != null && authenticatedUserId.equals(payment.getDriverUserId());
        if (!isAdmin && !isPassenger && !isDriver) {
            throw new AccessDeniedException("You can only view your own payments");
        }
        return PaymentResponse.from(payment);
    }

    /** Receipt is simply the completed payment record; kept as a separate method for clarity/extension. */
    public PaymentResponse getReceipt(String paymentId, String authenticatedUserId, String authenticatedRole) {
        PaymentResponse payment = getById(paymentId, authenticatedUserId, authenticatedRole);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentNotFoundException("No completed payment/receipt for id: " + paymentId);
        }
        return payment;
    }

    // ------------------------------------------------------------------ ADMIN

    /** Only ADMIN (enforced in SecurityConfig): every payment. */
    public List<PaymentResponse> listAll() {
        return paymentRepository.findAll().stream().map(PaymentResponse::from).collect(Collectors.toList());
    }

    /** Only ADMIN (enforced in SecurityConfig). */
    public void delete(String id) {
        findOrThrow(id);
        paymentRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ PASSENGER

    /** The passenger's own payments. */
    public List<PaymentResponse> getPassengerPayments(String pathUserId, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        return paymentRepository.findByPassengerId(pathUserId).stream()
                .map(PaymentResponse::from).collect(Collectors.toList());
    }

    /** The passenger's own payment for one ride. */
    public PaymentResponse getPassengerPaymentByRide(String pathUserId, String rideId, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        return pickOne(paymentRepository.findByRideId(rideId).stream()
                .filter(p -> pathUserId.equals(p.getPassengerId()))
                .collect(Collectors.toList()), rideId);
    }

    // ------------------------------------------------------------------ DRIVER

    /** The payments of rides the driver carried. */
    public List<PaymentResponse> getDriverPayments(String pathUserId, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        return paymentRepository.findByDriverUserId(pathUserId).stream()
                .map(PaymentResponse::from).collect(Collectors.toList());
    }

    /** One of the driver's own payments, by payment id. */
    public PaymentResponse getDriverPaymentById(String pathUserId, String id, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        Payment payment = findOrThrow(id);
        if (!pathUserId.equals(payment.getDriverUserId())) {
            throw new AccessDeniedException("You can only view your own payments");
        }
        return PaymentResponse.from(payment);
    }

    /** The driver's own payment for one ride. */
    public PaymentResponse getDriverPaymentByRide(String pathUserId, String rideId, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        return pickOne(paymentRepository.findByRideId(rideId).stream()
                .filter(p -> pathUserId.equals(p.getDriverUserId()))
                .collect(Collectors.toList()), rideId);
    }

    // ------------------------------------------------------------------ helpers

    /** A ride normally has one payment; if a failed attempt was retried, prefer the COMPLETED / newest one. */
    private PaymentResponse pickOne(List<Payment> candidates, String rideId) {
        return candidates.stream()
                .max(Comparator.comparing((Payment p) -> p.getStatus() == PaymentStatus.COMPLETED)
                        .thenComparing(Payment::getCreatedAt))
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException("No payment of yours found for ride: " + rideId));
    }

    private Payment findOrThrow(String id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    /** The {userId} in the URL must be the authenticated caller's own id. */
    private void requireSelf(String pathUserId, String authenticatedUserId) {
        if (!pathUserId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only access your own payments");
        }
    }
}
