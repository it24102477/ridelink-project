package com.ridelink.account.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ridelink.account.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Synchronous REST client to ride-management-service. Before an account is deleted we must know
 * whether the user still has a ride that is not COMPLETED or CANCELLED - and we can not delete
 * safely if we cannot find out, so a failure here blocks the deletion (502) instead of skipping the check.
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

    /** The caller's own bearer token (the user, or an ADMIN) is forwarded. */
    public boolean hasOpenRides(String userId, String callerAuthorizationHeader) {
        requireToken(callerAuthorizationHeader);
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

    static void requireToken(String header) {
        if (header == null || header.isBlank()) {
            throw new AccessDeniedException("Missing or empty bearer token");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenRides {
        public boolean hasOpenRides;
        public long openRideCount;
        public String userId;
    }
}
