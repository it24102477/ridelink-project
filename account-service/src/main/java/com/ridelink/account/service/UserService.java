package com.ridelink.account.service;

import com.ridelink.account.client.DriverServiceClient;
import com.ridelink.account.client.RideServiceClient;
import com.ridelink.account.dto.*;
import com.ridelink.account.exception.*;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final RideServiceClient rideServiceClient;
    private final DriverServiceClient driverServiceClient;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                        SequenceGeneratorService sequenceGeneratorService,
                        RideServiceClient rideServiceClient, DriverServiceClient driverServiceClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.rideServiceClient = rideServiceClient;
        this.driverServiceClient = driverServiceClient;
    }

    // ------------------------------------------------------------ public

    public UserResponse register(RegisterRequest req) {
        if (req.getRole() == Role.ADMIN) {
            throw new RoleNotAllowedException();
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException(req.getEmail());
        }
        User user = new User(
                req.getFullName(),
                req.getEmail(),
                passwordEncoder.encode(req.getPassword()),
                req.getPhoneNumber(),
                req.getRole()
        );
        user.setId(sequenceGeneratorService.nextId("users"));
        return UserResponse.from(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountSuspendedException();
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getRole().name());
    }

    // ------------------------------------------------------- any logged-in user

    /** Own profile. */
    public UserResponse getMe(String authenticatedUserId) {
        return userRepository.findById(authenticatedUserId)
                .map(UserResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(authenticatedUserId));
    }

    public UserResponse updateProfile(String id, UpdateProfileRequest req, String authenticatedUserId, String authenticatedRole) {
        requireOwnerOrAdmin(id, authenticatedUserId, authenticatedRole);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName());
        }
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(req.getPhoneNumber());
        }
        return UserResponse.from(userRepository.save(user));
    }

    public void changePassword(String authenticatedUserId, ChangePasswordRequest req) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new AccountNotFoundException(authenticatedUserId));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // -------------------------------------------------------------- admin only

    public List<UserResponse> getAll(String authenticatedRole) {
        requireAdmin(authenticatedRole);
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getById(String id, String authenticatedRole) {
        requireAdmin(authenticatedRole);
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public UserResponse updateStatus(String id, UpdateStatusRequest req, String authenticatedRole) {
        requireAdmin(authenticatedRole);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        user.setStatus(req.getStatus());
        return UserResponse.from(userRepository.save(user));
    }

    // ------------------------------------------------------------ account deletion

    static final String ADMIN_DELETE_BLOCKED = "Can not delete his ride not completed";
    static final String OWN_DELETE_BLOCKED = "Can not delete your ride not completed";

    /**
     * ADMIN deletes a PASSENGER or DRIVER account - only if every ride of that user is COMPLETED
     * or CANCELLED. Deleting a DRIVER account also deletes the driver's profile in
     * driver-vehicle-service.
     */
    public void deleteAccountAsAdmin(String id, String callerAuthorizationHeader, String authenticatedRole) {
        requireAdmin(authenticatedRole);
        User user = userRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        if (user.getRole() == Role.ADMIN) {
            throw new RoleNotAllowedException("Can not delete an ADMIN account");
        }
        deleteAccount(user, callerAuthorizationHeader, ADMIN_DELETE_BLOCKED);
    }

    /**
     * A PASSENGER or DRIVER deletes their own account - same rule. A DRIVER's profile in
     * driver-vehicle-service is deleted with it.
     */
    public void deleteOwnAccount(String authenticatedUserId, String callerAuthorizationHeader, String authenticatedRole) {
        if ("ADMIN".equals(authenticatedRole)) {
            throw new RoleNotAllowedException("Can not delete an ADMIN account");
        }
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new AccountNotFoundException(authenticatedUserId));
        deleteAccount(user, callerAuthorizationHeader, OWN_DELETE_BLOCKED);
    }

    private void deleteAccount(User user, String callerAuthorizationHeader, String blockedMessage) {
        if (rideServiceClient.hasOpenRides(user.getId(), callerAuthorizationHeader)) {
            throw new RideNotCompletedException(blockedMessage);
        }
        // Profile first, account last: if the profile call fails the account still exists and the
        // delete can simply be retried, instead of leaving a driver profile with no account.
        if (user.getRole() == Role.DRIVER) {
            driverServiceClient.deleteDriverProfile(user.getId(), callerAuthorizationHeader, blockedMessage);
        }
        userRepository.deleteById(user.getId());
    }

    // ----------------------------------------------------------------- helpers

    private void requireAdmin(String authenticatedRole) {
        if (!"ADMIN".equals(authenticatedRole)) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    /** Passengers/drivers may only edit their own account; ADMIN may access any. */
    private void requireOwnerOrAdmin(String targetId, String authenticatedUserId, String authenticatedRole) {
        boolean isAdmin = "ADMIN".equals(authenticatedRole);
        if (!isAdmin && !targetId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only access your own account");
        }
    }
}