package com.ridelink.account.service;

import com.ridelink.account.client.DriverServiceClient;
import com.ridelink.account.client.RideServiceClient;
import com.ridelink.account.dto.ChangePasswordRequest;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.exception.InvalidPasswordException;
import com.ridelink.account.exception.RideNotCompletedException;
import com.ridelink.account.exception.RoleNotAllowedException;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private SequenceGeneratorService sequenceGeneratorService;
    @Mock private RideServiceClient rideServiceClient;
    @Mock private DriverServiceClient driverServiceClient;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, passwordEncoder, jwtUtil, sequenceGeneratorService,
                rideServiceClient, driverServiceClient);
        lenient().when(sequenceGeneratorService.nextId(any())).thenReturn("1");
    }

    // ---------------------------------------------------------------- register

    @Test
    void register_savesNewUser_whenEmailNotTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Nimal Perera");
        req.setEmail("nimal@example.com");
        req.setPassword("password123");
        req.setPhoneNumber("0771234567");
        req.setRole(Role.PASSENGER);

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("u1");
            return u;
        });

        var result = userService.register(req);

        assertEquals("nimal@example.com", result.getEmail());
        assertEquals(Role.PASSENGER, result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenEmailAlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@example.com");
        req.setRole(Role.PASSENGER);

        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsRoleNotAllowed_whenRoleIsAdmin() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("admin@example.com");
        req.setRole(Role.ADMIN);

        assertThrows(RoleNotAllowedException.class, () -> userService.register(req));
        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------------------------- login

    @Test
    void login_throwsInvalidCredentials_whenPasswordWrong() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nimal@example.com");
        req.setPassword("wrong");

        User existing = new User("Nimal", "nimal@example.com", "hashed", "0771234567", Role.PASSENGER);
        when(userRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(req));
    }

    @Test
    void login_throwsInvalidCredentials_whenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@example.com");
        req.setPassword("whatever");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(req));
    }

    // --------------------------------------------------------- getById (admin)

    @Test
    void getById_succeeds_forAdmin() {
        User existing = new User("Nimal", "nimal@example.com", "hashed", "0771234567", Role.PASSENGER);
        existing.setId("1");
        when(userRepository.findById("1")).thenReturn(Optional.of(existing));

        var result = userService.getById("1", "ADMIN");

        assertEquals("1", result.getId());
    }

    @Test
    void getById_throwsAccessDenied_forPassenger() {
        assertThrows(AccessDeniedException.class, () -> userService.getById("1", "PASSENGER"));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getById_throwsAccessDenied_forDriver() {
        assertThrows(AccessDeniedException.class, () -> userService.getById("1", "DRIVER"));
        verify(userRepository, never()).findById(any());
    }

    // ------------------------------------------------------------------ getMe

    @Test
    void getMe_returnsOwnProfile() {
        User existing = new User("Nimal", "nimal@example.com", "hashed", "0771234567", Role.PASSENGER);
        existing.setId("1");
        when(userRepository.findById("1")).thenReturn(Optional.of(existing));

        var result = userService.getMe("1");

        assertEquals("1", result.getId());
        assertEquals("nimal@example.com", result.getEmail());
    }

    // ---------------------------------------------------------- updateProfile

    @Test
    void updateProfile_throwsAccessDenied_whenCallerDoesNotOwnTheAccount() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");

        assertThrows(AccessDeniedException.class, () -> userService.updateProfile("1", req, "2", "PASSENGER"));
        verify(userRepository, never()).save(any());
    }

    // --------------------------------------------------------- changePassword

    @Test
    void changePassword_updatesHash_whenCurrentPasswordCorrect() {
        User existing = new User("Nimal", "nimal@example.com", "oldHash", "0771234567", Role.PASSENGER);
        existing.setId("1");
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPassword1");
        req.setNewPassword("newPassword1");

        when(userRepository.findById("1")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("oldPassword1", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("newPassword1", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("newPassword1")).thenReturn("newHash");

        userService.changePassword("1", req);

        assertEquals("newHash", existing.getPasswordHash());
        verify(userRepository).save(existing);
    }

    @Test
    void changePassword_throws_whenCurrentPasswordWrong() {
        User existing = new User("Nimal", "nimal@example.com", "oldHash", "0771234567", Role.PASSENGER);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("newPassword1");

        when(userRepository.findById("1")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "oldHash")).thenReturn(false);

        assertThrows(InvalidPasswordException.class, () -> userService.changePassword("1", req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_throws_whenNewPasswordSameAsCurrent() {
        User existing = new User("Nimal", "nimal@example.com", "oldHash", "0771234567", Role.PASSENGER);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("samePassword1");
        req.setNewPassword("samePassword1");

        when(userRepository.findById("1")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("samePassword1", "oldHash")).thenReturn(true);

        assertThrows(InvalidPasswordException.class, () -> userService.changePassword("1", req));
        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- delete account

    private User userWithId(String id, Role role) {
        User u = new User("Nimal", id + "@example.com", "hash", "0771234567", role);
        u.setId(id);
        return u;
    }

    @Test
    void adminDeletesPassenger_whenAllRidesFinished() {
        when(userRepository.findById("p1")).thenReturn(Optional.of(userWithId("p1", Role.PASSENGER)));
        when(rideServiceClient.hasOpenRides("p1", "Bearer admin")).thenReturn(false);

        userService.deleteAccountAsAdmin("p1", "Bearer admin", "ADMIN");

        verify(userRepository).deleteById("p1");
        verifyNoInteractions(driverServiceClient);
    }

    @Test
    void adminDeletesDriver_alsoDeletesTheDriverProfile() {
        when(userRepository.findById("d1")).thenReturn(Optional.of(userWithId("d1", Role.DRIVER)));
        when(rideServiceClient.hasOpenRides("d1", "Bearer admin")).thenReturn(false);

        userService.deleteAccountAsAdmin("d1", "Bearer admin", "ADMIN");

        verify(driverServiceClient).deleteDriverProfile("d1", "Bearer admin", "Can not delete his ride not completed");
        verify(userRepository).deleteById("d1");
    }

    @Test
    void adminDelete_isRejected_whenARideIsNotCompleted() {
        when(userRepository.findById("p1")).thenReturn(Optional.of(userWithId("p1", Role.PASSENGER)));
        when(rideServiceClient.hasOpenRides("p1", "Bearer admin")).thenReturn(true);

        var ex = assertThrows(RideNotCompletedException.class,
                () -> userService.deleteAccountAsAdmin("p1", "Bearer admin", "ADMIN"));

        assertEquals("Can not delete his ride not completed", ex.getMessage());
        verify(userRepository, never()).deleteById(any());
        verifyNoInteractions(driverServiceClient);
    }

    @Test
    void adminDelete_isRejected_forADriverWithARideNotCompleted_andKeepsTheProfile() {
        when(userRepository.findById("d1")).thenReturn(Optional.of(userWithId("d1", Role.DRIVER)));
        when(rideServiceClient.hasOpenRides("d1", "Bearer admin")).thenReturn(true);

        assertThrows(RideNotCompletedException.class,
                () -> userService.deleteAccountAsAdmin("d1", "Bearer admin", "ADMIN"));

        verify(driverServiceClient, never()).deleteDriverProfile(any(), any(), any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void adminDelete_isRejected_forAnAdminAccount() {
        when(userRepository.findById("a1")).thenReturn(Optional.of(userWithId("a1", Role.ADMIN)));

        assertThrows(RoleNotAllowedException.class,
                () -> userService.deleteAccountAsAdmin("a1", "Bearer admin", "ADMIN"));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void adminDelete_throwsAccessDenied_forNonAdmins() {
        assertThrows(AccessDeniedException.class,
                () -> userService.deleteAccountAsAdmin("p1", "Bearer x", "PASSENGER"));
        verifyNoInteractions(rideServiceClient);
    }

    @Test
    void passengerDeletesOwnAccount_whenAllRidesFinished() {
        when(userRepository.findById("p1")).thenReturn(Optional.of(userWithId("p1", Role.PASSENGER)));
        when(rideServiceClient.hasOpenRides("p1", "Bearer p")).thenReturn(false);

        userService.deleteOwnAccount("p1", "Bearer p", "PASSENGER");

        verify(userRepository).deleteById("p1");
    }

    @Test
    void ownDelete_isRejected_withYourMessage_whenARideIsNotCompleted() {
        when(userRepository.findById("p1")).thenReturn(Optional.of(userWithId("p1", Role.PASSENGER)));
        when(rideServiceClient.hasOpenRides("p1", "Bearer p")).thenReturn(true);

        var ex = assertThrows(RideNotCompletedException.class,
                () -> userService.deleteOwnAccount("p1", "Bearer p", "PASSENGER"));

        assertEquals("Can not delete your ride not completed", ex.getMessage());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void driverDeletesOwnAccount_alsoDeletesTheDriverProfile() {
        when(userRepository.findById("d1")).thenReturn(Optional.of(userWithId("d1", Role.DRIVER)));
        when(rideServiceClient.hasOpenRides("d1", "Bearer d")).thenReturn(false);

        userService.deleteOwnAccount("d1", "Bearer d", "DRIVER");

        verify(driverServiceClient).deleteDriverProfile("d1", "Bearer d", "Can not delete your ride not completed");
        verify(userRepository).deleteById("d1");
    }

    @Test
    void ownDelete_isRejected_forAnAdmin() {
        assertThrows(RoleNotAllowedException.class, () -> userService.deleteOwnAccount("a1", "Bearer a", "ADMIN"));
        verifyNoInteractions(rideServiceClient);
    }
}
