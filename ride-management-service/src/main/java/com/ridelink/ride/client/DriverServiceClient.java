package com.ridelink.ride.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Synchronous REST client to driver-vehicle-service.
 *
 * Justification (report section: communication-interface comparison): driver assignment
 * is on the passenger-facing request path and needs an immediate answer (is a driver
 * available right now?), so synchronous REST is used here rather than asynchronous
 * messaging - the caller cannot proceed without a timely response.
 */
@Component
public class DriverServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DriverServiceClient(RestTemplate restTemplate,
                                @Value("${ridelink.services.driver-service-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<AvailableDriverDto> findAvailableDrivers(String serviceArea) {
        try {
            String url = baseUrl + "/api/drivers/available?serviceArea=" + serviceArea;
            AvailableDriverDto[] result = restTemplate.getForObject(url, AvailableDriverDto[].class);
            return result == null ? List.of() : List.of(result);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("driver-vehicle-service", ex);
        }
    }

    /**
     * Looks up a driver's operational profile by id. Used on the ride-accept path to
     * confirm that the driverId a caller claims a ride with is really theirs (userId
     * match), and that they are currently AVAILABLE and in the ride's service area,
     * before we attempt the atomic accept.
     *
     * GET /api/drivers/{id} on driver-vehicle-service requires a DRIVER-role token, so
     * the caller's own bearer token (the one they used to call ride-management-service)
     * is forwarded as-is - it is the same account-service-issued JWT, valid across
     * both services.
     *
     * @return the profile, or {@code null} if driver-vehicle-service reports no such driver
     */
    // public DriverProfileDto getDriver(String driverId, String callerAuthorizationHeader) {
    //     try {
    //         String url = baseUrl + "/api/drivers/" + driverId;
    //         HttpHeaders headers = new HttpHeaders();
    //         headers.set("Authorization", callerAuthorizationHeader);
    //         var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), DriverProfileDto.class);
    //         return response.getBody();
    //     } catch (HttpClientErrorException.NotFound ex) {
    //         return null;
    //     } catch (RestClientException ex) {
    //         throw new DownstreamServiceException("driver-vehicle-service", ex);
    //     }
    // }


    public DriverProfileDto getDriver(String driverId, String callerAuthorizationHeader) {
    if (callerAuthorizationHeader == null || callerAuthorizationHeader.isBlank()) {
        throw new AccessDeniedException("Missing or empty bearer token");
    }
    try {
        String url = baseUrl + "/api/drivers/" + driverId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", callerAuthorizationHeader);
        var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), DriverProfileDto.class);
        return response.getBody();
    } catch (HttpClientErrorException.NotFound ex) {
        return null;
    } catch (RestClientException ex) {
        throw new DownstreamServiceException("driver-vehicle-service", ex);
    }
}

    /**
     * Sets the driver's availability to BUSY. Called right after a driver wins a ride.
     * driver-vehicle-service only lets a driver change their own availability
     * (PUT /api/{userId}/drivers/{id}/availability, DRIVER role, {userId} == token subject),
     * so the accepting driver's own bearer token is forwarded.
     */
    public void markBusy(String driverId, String driverUserId, String callerAuthorizationHeader) {
        setAvailability(driverId, driverUserId, "BUSY", callerAuthorizationHeader);
    }

    /** Sets the driver's availability to AVAILABLE. Called right after their ride completes. */
    public void markAvailable(String driverId, String driverUserId, String callerAuthorizationHeader) {
        setAvailability(driverId, driverUserId, "AVAILABLE", callerAuthorizationHeader);
    }

    private void setAvailability(String driverId, String driverUserId, String availability,
                                 String callerAuthorizationHeader) {
        if (callerAuthorizationHeader == null || callerAuthorizationHeader.isBlank()) {
            throw new AccessDeniedException("Missing or empty bearer token");
        }
        try {
            String url = baseUrl + "/api/" + driverUserId + "/drivers/" + driverId + "/availability";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", callerAuthorizationHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(Map.of("availability", availability), headers), Void.class);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("driver-vehicle-service", ex);
        }
    }
}
