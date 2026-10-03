package com.ridelink.fare.service;

import com.ridelink.fare.dto.CreatePaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.exception.PaymentNotFoundException;
import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private SequenceGeneratorService sequenceGeneratorService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(paymentRepository, "LKR", sequenceGeneratorService);
        lenient().when(sequenceGeneratorService.nextId(any())).thenReturn("1");
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void recordPayment_completes_whenAmountWithinAllowedRange() {
        CreatePaymentRequest req = request("ride-1", "passenger-1", 1500.0);

        PaymentResponse response = paymentService.recordPayment(req);

        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertNull(response.getFailureReason());
    }

    @Test
    void recordPayment_simulatesFailure_whenAmountAboveAllowedRange() {
        CreatePaymentRequest req = request("ride-2", "passenger-1", 99_999.0);

        PaymentResponse response = paymentService.recordPayment(req);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getFailureReason());
    }

    @Test
    void recordPayment_simulatesFailure_whenAmountBelowAllowedRange() {
        CreatePaymentRequest req = request("ride-3", "passenger-1", 10.0);

        PaymentResponse response = paymentService.recordPayment(req);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
    }

    @Test
    void getById_throwsNotFound_whenPaymentDoesNotExist() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getById("missing", "passenger-1", "PASSENGER"));
    }

    @Test
    void getById_succeeds_forOwningPassenger() {
        Payment payment = new Payment("ride-1", "passenger-1", 1500.0, "LKR", PaymentMethod.SIMULATED_CARD);
        payment.setId("1");
        payment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById("1")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getById("1", "passenger-1", "PASSENGER");

        assertEquals("passenger-1", response.getPassengerId());
    }

    @Test
    void getById_throwsAccessDenied_forDifferentPassenger() {
        Payment payment = new Payment("ride-1", "passenger-1", 1500.0, "LKR", PaymentMethod.SIMULATED_CARD);
        payment.setId("1");
        when(paymentRepository.findById("1")).thenReturn(Optional.of(payment));

        assertThrows(AccessDeniedException.class,
                () -> paymentService.getById("1", "some-other-passenger", "PASSENGER"));
    }

    @Test
    void getById_succeeds_forAdmin_regardlessOfOwnership() {
        Payment payment = new Payment("ride-1", "passenger-1", 1500.0, "LKR", PaymentMethod.SIMULATED_CARD);
        payment.setId("1");
        when(paymentRepository.findById("1")).thenReturn(Optional.of(payment));

        assertDoesNotThrow(() -> paymentService.getById("1", "admin-user", "ADMIN"));
    }

    @Test
    void getReceipt_throwsNotFound_whenPaymentFailed() {
        Payment payment = new Payment("ride-1", "passenger-1", 1500.0, "LKR", PaymentMethod.SIMULATED_CARD);
        payment.setId("1");
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById("1")).thenReturn(Optional.of(payment));

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getReceipt("1", "passenger-1", "PASSENGER"));
    }

    // ---------------------------------------------------------------- driver on the payment

    private Payment payment(String id, String rideId, String passengerId, String driverUserId, PaymentStatus status) {
        Payment p = new Payment(rideId, passengerId, 1500.0, "LKR", PaymentMethod.SIMULATED_CARD);
        p.setId(id);
        p.setDriverUserId(driverUserId);
        p.setStatus(status);
        return p;
    }

    @Test
    void recordPayment_remembersWhichDriverWasPaid() {
        CreatePaymentRequest req = request("ride-1", "passenger-1", 1500.0);
        req.setDriverUserId("driver-user-1");

        assertEquals("driver-user-1", paymentService.recordPayment(req).getDriverUserId());
    }

    @Test
    void getById_succeeds_forThePaidDriver_butNotForAnotherDriver() {
        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment("1", "ride-1", "passenger-1", "driver-user-1", PaymentStatus.COMPLETED)));

        assertDoesNotThrow(() -> paymentService.getById("1", "driver-user-1", "DRIVER"));
        assertThrows(AccessDeniedException.class, () -> paymentService.getById("1", "driver-user-2", "DRIVER"));
    }

    // ---------------------------------------------------------------- ADMIN

    @Test
    void listAll_returnsEveryPayment() {
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment("1", "ride-1", "p1", "d1", PaymentStatus.COMPLETED),
                payment("2", "ride-2", "p2", "d2", PaymentStatus.COMPLETED)));

        assertEquals(2, paymentService.listAll().size());
    }

    @Test
    void delete_removesTheExistingPayment() {
        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment("1", "ride-1", "p1", "d1", PaymentStatus.COMPLETED)));

        paymentService.delete("1");

        verify(paymentRepository).deleteById("1");
    }

    @Test
    void delete_throwsNotFound_whenPaymentDoesNotExist() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.delete("missing"));
        verify(paymentRepository, never()).deleteById(any());
    }

    // ---------------------------------------------------------------- PASSENGER

    @Test
    void getPassengerPayments_returnsOnlyTheCallersPayments() {
        when(paymentRepository.findByPassengerId("passenger-1"))
                .thenReturn(List.of(payment("1", "ride-1", "passenger-1", "d1", PaymentStatus.COMPLETED)));

        assertEquals(1, paymentService.getPassengerPayments("passenger-1", "passenger-1").size());
    }

    @Test
    void getPassengerPayments_throwsAccessDenied_forAnotherUsersId() {
        assertThrows(AccessDeniedException.class,
                () -> paymentService.getPassengerPayments("passenger-1", "someone-else"));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getPassengerPaymentByRide_returnsTheCallersPayment() {
        when(paymentRepository.findByRideId("ride-1")).thenReturn(List.of(
                payment("1", "ride-1", "passenger-1", "d1", PaymentStatus.COMPLETED)));

        assertEquals("1", paymentService.getPassengerPaymentByRide("passenger-1", "ride-1", "passenger-1").getId());
    }

    @Test
    void getPassengerPaymentByRide_throwsNotFound_forSomeoneElsesRide() {
        when(paymentRepository.findByRideId("ride-1")).thenReturn(List.of(
                payment("1", "ride-1", "passenger-1", "d1", PaymentStatus.COMPLETED)));

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getPassengerPaymentByRide("passenger-2", "ride-1", "passenger-2"));
    }

    @Test
    void getPassengerPaymentByRide_prefersTheCompletedPayment_whenAnEarlierAttemptFailed() {
        when(paymentRepository.findByRideId("ride-1")).thenReturn(List.of(
                payment("1", "ride-1", "passenger-1", "d1", PaymentStatus.FAILED),
                payment("2", "ride-1", "passenger-1", "d1", PaymentStatus.COMPLETED)));

        assertEquals("2", paymentService.getPassengerPaymentByRide("passenger-1", "ride-1", "passenger-1").getId());
    }

    // ---------------------------------------------------------------- DRIVER

    @Test
    void getDriverPayments_returnsOnlyTheCallersPayments() {
        when(paymentRepository.findByDriverUserId("driver-user-1"))
                .thenReturn(List.of(payment("1", "ride-1", "p1", "driver-user-1", PaymentStatus.COMPLETED)));

        assertEquals(1, paymentService.getDriverPayments("driver-user-1", "driver-user-1").size());
    }

    @Test
    void getDriverPayments_throwsAccessDenied_forAnotherUsersId() {
        assertThrows(AccessDeniedException.class,
                () -> paymentService.getDriverPayments("driver-user-1", "someone-else"));
    }

    @Test
    void getDriverPaymentById_throwsAccessDenied_whenPaymentBelongsToAnotherDriver() {
        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment("1", "ride-1", "p1", "driver-user-2", PaymentStatus.COMPLETED)));

        assertThrows(AccessDeniedException.class,
                () -> paymentService.getDriverPaymentById("driver-user-1", "1", "driver-user-1"));
    }

    @Test
    void getDriverPaymentById_returnsTheDriversOwnPayment() {
        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment("1", "ride-1", "p1", "driver-user-1", PaymentStatus.COMPLETED)));

        assertEquals("1", paymentService.getDriverPaymentById("driver-user-1", "1", "driver-user-1").getId());
    }

    @Test
    void getDriverPaymentByRide_returnsTheDriversOwnPayment_andHidesOthers() {
        when(paymentRepository.findByRideId("ride-1")).thenReturn(List.of(
                payment("1", "ride-1", "p1", "driver-user-1", PaymentStatus.COMPLETED)));

        assertEquals("1", paymentService.getDriverPaymentByRide("driver-user-1", "ride-1", "driver-user-1").getId());
        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getDriverPaymentByRide("driver-user-2", "ride-1", "driver-user-2"));
    }

    private CreatePaymentRequest request(String rideId, String passengerId, double amount) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setRideId(rideId);
        req.setPassengerId(passengerId);
        req.setAmount(amount);
        req.setMethod(PaymentMethod.SIMULATED_CARD);
        return req;
    }
}
