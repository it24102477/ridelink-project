package com.ridelink.driver.service;

import com.ridelink.driver.client.GeocodingClient;
import com.ridelink.driver.client.RideServiceClient;
import com.ridelink.driver.dto.*;
import com.ridelink.driver.exception.DriverNotFoundException;
import com.ridelink.driver.exception.DriverProfileAlreadyExistsException;
import com.ridelink.driver.exception.DriverProfileConflictException;
import com.ridelink.driver.exception.LocationOutsideServiceAreaException;
import com.ridelink.driver.exception.RideNotCompletedException;
import com.ridelink.driver.model.*;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.validation.SriLankaDistricts;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final GeocodingClient geocodingClient;
    private final RideServiceClient rideServiceClient;

    public DriverService(DriverRepository driverRepository, SequenceGeneratorService sequenceGeneratorService,
                         GeocodingClient geocodingClient, RideServiceClient rideServiceClient) {
        this.driverRepository = driverRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.geocodingClient = geocodingClient;
        this.rideServiceClient = rideServiceClient;
    }

    // ---------------------------------------------------------------------
    // DRIVER-only operations (DriverController: /api/{userId}/drivers/...)
    // ---------------------------------------------------------------------

    public DriverResponse createProfile(String pathUserId, CreateDriverRequest req, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);

        driverRepository.findByUserId(pathUserId).ifPresent(d -> {
            throw new DriverProfileAlreadyExistsException(pathUserId);
        });
        if (driverRepository.existsByLicenseNumber(req.getLicenseNumber())) {
            throw DriverProfileConflictException.duplicateLicenseNumber(req.getLicenseNumber());
        }
        String plate = normalizePlate(req.getVehicle().getPlateNumber());
        if (driverRepository.existsByVehicle_PlateNumber(plate)) {
            throw DriverProfileConflictException.duplicatePlateNumber(plate);
        }

        Vehicle vehicle = new Vehicle(
                req.getVehicle().getMake(),
                req.getVehicle().getModel(),
                plate,
                req.getVehicle().getColor(),
                req.getVehicle().getCapacity()
        );
        Driver driver = new Driver(pathUserId, req.getLicenseNumber(), vehicle,
                SriLankaDistricts.canonical(req.getServiceArea()));
        driver.setId(sequenceGeneratorService.nextId("drivers"));
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateVehicle(String pathUserId, String id, VehicleDto req, String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        Driver driver = findOrThrow(id);
        requireOwner(driver, authenticatedUserId);

        String plate = normalizePlate(req.getPlateNumber());
        boolean plateChanged = driver.getVehicle() == null
                || !plate.equals(driver.getVehicle().getPlateNumber());
        if (plateChanged && driverRepository.existsByVehicle_PlateNumber(plate)) {
            throw DriverProfileConflictException.duplicatePlateNumber(plate);
        }

        driver.setVehicle(new Vehicle(req.getMake(), req.getModel(), plate, req.getColor(), req.getCapacity()));
        driver.setUpdatedAt(Instant.now());
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateServiceArea(String pathUserId, String id, UpdateServiceAreaRequest req,
                                            String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        Driver driver = findOrThrow(id);
        requireOwner(driver, authenticatedUserId);

        driver.setServiceArea(SriLankaDistricts.canonical(req.getServiceArea()));
        driver.setUpdatedAt(Instant.now());
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateAvailability(String pathUserId, String id, UpdateAvailabilityRequest req,
                                             String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        Driver driver = findOrThrow(id);
        requireOwner(driver, authenticatedUserId);

        driver.setAvailability(req.getAvailability());
        driver.setUpdatedAt(Instant.now());
        return DriverResponse.from(driverRepository.save(driver));
    }

    public DriverResponse updateLocation(String pathUserId, String id, UpdateLocationRequest req,
                                         String authenticatedUserId) {
        requireSelf(pathUserId, authenticatedUserId);
        Driver driver = findOrThrow(id);
        requireOwner(driver, authenticatedUserId);

        // The driver types an address; latitude/longitude are resolved from it (free OSM Nominatim).
        GeoPoint location = geocodingClient.geocode(req.getAddress());

        // The location must be inside the driver's own service area (district).
        String serviceArea = driver.getServiceArea();
        if (location.getDistrict() == null) {
            throw LocationOutsideServiceAreaException.unverifiable(serviceArea);
        }
        if (serviceArea == null || !serviceArea.equalsIgnoreCase(location.getDistrict())) {
            throw new LocationOutsideServiceAreaException(serviceArea);
        }
        driver.setCurrentLocation(location);
        driver.setUpdatedAt(Instant.now());
        return DriverResponse.from(driverRepository.save(driver));
    }

    // ---------------------------------------------------------------------
    // ADMIN-only operations (AdminController: /api/admin/drivers)
    // ---------------------------------------------------------------------

    public List<DriverResponse> listAll() {
        return driverRepository.findAll().stream()
                .map(DriverResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a driver profile. Allowed only when every ride the driver is on is COMPLETED or
     * CANCELLED; otherwise nothing is deleted and the caller gets a validation message.
     */
    public void deleteProfileAsAdmin(String id, String callerAuthorizationHeader) {
        Driver driver = findOrThrow(id);
        if (rideServiceClient.hasOpenRides(driver.getUserId(), callerAuthorizationHeader)) {
            throw new RideNotCompletedException("Can not delete his ride not completed");
        }
        driverRepository.deleteById(id);
    }

    // ---------------------------------------------------------------------
    // PASSENGER / DRIVER / ADMIN operations (UserController: /api/drivers/...)
    // ---------------------------------------------------------------------

    public DriverResponse getById(String id) {
        return driverRepository.findById(id)
                .map(DriverResponse::from)
                .orElseThrow(() -> new DriverNotFoundException(id));
    }

    /**
     * Removes the driver profile that belongs to an account. Called by account-service when a
     * DRIVER account is deleted (by the driver, or by an ADMIN): account-service has already
     * checked the driver's rides, and this re-checks them, so a profile is never removed while a
     * ride is open. A driver may only remove their own profile; an ADMIN may remove any.
     * A driver who never created a profile is fine - there is simply nothing to delete.
     */
    public void deleteProfileByUserId(String userId, String callerAuthorizationHeader,
                                      String authenticatedUserId, String authenticatedRole) {
        if (!"ADMIN".equals(authenticatedRole) && !userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only delete your own driver profile");
        }
        driverRepository.findByUserId(userId).ifPresent(driver -> {
            if (rideServiceClient.hasOpenRides(userId, callerAuthorizationHeader)) {
                String who = userId.equals(authenticatedUserId) ? "your" : "his";
                throw new RideNotCompletedException("Can not delete " + who + " ride not completed");
            }
            driverRepository.deleteById(driver.getId());
        });
    }

    /**
     * Returns available drivers in a service area. Called synchronously (REST) by
     * ride-management-service when it needs to assign a driver to a new ride request.
     */
    public List<DriverResponse> findAvailableDrivers(String serviceArea) {
        List<Driver> drivers = (serviceArea == null || serviceArea.isBlank())
                ? driverRepository.findByAvailability(AvailabilityStatus.AVAILABLE)
                : driverRepository.findByAvailabilityAndServiceArea(
                        AvailabilityStatus.AVAILABLE, SriLankaDistricts.canonical(serviceArea));
        return drivers.stream().map(DriverResponse::from).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** The {userId} in the URL must be the authenticated caller's own id. */
    private void requireSelf(String pathUserId, String authenticatedUserId) {
        if (!pathUserId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only access your own driver resources");
        }
    }

    /** Drivers may only modify their own profile - never another driver's record. */
    private void requireOwner(Driver driver, String authenticatedUserId) {
        if (!driver.getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only modify your own driver profile");
        }
    }

    /** Uppercase so "abc123" and "ABC123" cannot both be registered. */
    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase();
    }

    private Driver findOrThrow(String id) {
        return driverRepository.findById(id).orElseThrow(() -> new DriverNotFoundException(id));
    }
}