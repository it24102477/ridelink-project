package com.ridelink.driver.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ridelink.driver.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Synchronous REST client to ride-management-service. Used before a driver profile is deleted,
 * to make sure every ride the driver is on is COMPLETED or CANCELLED.
 */
@Component
public class RideServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RideServiceClient(RestTemplate restTemplate,
                             @Value("${ridelink.services.ride-service-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * True when the user still has a ride that is not COMPLETED or CANCELLED. The caller's own
     * bearer token is forwarded (ride-management-service lets a user, or an ADMIN, ask this).
     */
    public boolean hasOpenRides(String userId, String callerAuthorizationHeader) {
        if (callerAuthorizationHeader == null || callerAuthorizationHeader.isBlank()) {
            throw new AccessDeniedException("Missing or empty bearer token");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", callerAuthorizationHeader);
            var response = restTemplate.exchange(baseUrl + "/api/rides/users/" + userId + "/open-rides",
                    HttpMethod.GET, new HttpEntity<>(headers), OpenRides.class);
            OpenRides body = response.getBody();
            if (body == null) throw new DownstreamServiceException("ride-management-service", null);
            return body.hasOpenRides;
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("ride-management-service", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenRides {
        public boolean hasOpenRides;
        public long openRideCount;
        public String userId;
    }
}
