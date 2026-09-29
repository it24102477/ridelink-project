package com.ridelink.account.client;

import com.ridelink.account.exception.DownstreamServiceException;
import com.ridelink.account.exception.RideNotCompletedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Synchronous REST client to driver-vehicle-service: removes a driver's profile with their account. */
@Component
public class DriverServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DriverServiceClient(RestTemplate restTemplate,
                               @Value("${ridelink.services.driver-service-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Deletes the driver profile that belongs to {@code userId}. The caller's own token is
     * forwarded: driver-vehicle-service accepts the driver deleting their own profile, or an
     * ADMIN deleting any. It answers 204 even when the driver never created a profile.
     */
    public void deleteDriverProfile(String userId, String callerAuthorizationHeader, String rideNotCompletedMessage) {
        RideServiceClient.requireToken(callerAuthorizationHeader);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", callerAuthorizationHeader);
            restTemplate.exchange(baseUrl + "/api/drivers/user/" + userId,
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (HttpClientErrorException.Conflict ex) {
            // A ride became active between our own check and this call.
            throw new RideNotCompletedException(rideNotCompletedMessage);
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("driver-vehicle-service", ex);
        }
    }
}
