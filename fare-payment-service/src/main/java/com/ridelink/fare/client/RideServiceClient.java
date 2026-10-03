package com.ridelink.fare.client;

import com.ridelink.fare.exception.RideNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Synchronous REST client to ride-management-service, used by the driver-facing
 * "complete ride and pay" flow (PaymentController#completeRideAndPay ->
 * RideCompletionService): fare-payment-service authenticates the driver against the
 * ride and later hands ride-management-service the finished result, so both calls
 * need an immediate answer before the flow can continue.
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
     * Looks up a ride by id, forwarding the caller's own bearer token (the one they used
     * to call fare-payment-service - it is the same account-service-issued JWT, valid
     * across both services). ride-management-service's GET /api/rides/{id} only allows
     * the ride's passenger, its assigned driver, or an ADMIN to view it, so a 403 here
     * means this caller is not the driver assigned to this ride - exactly the
     * "authenticate driver against ride id" check this flow needs.
     */
    public RideDto getRide(String rideId, String callerAuthorizationHeader) {
        try {
            String url = baseUrl + "/api/rides/" + rideId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", callerAuthorizationHeader);
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), RideDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RideNotFoundException(rideId);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new AccessDeniedException("You are not the driver assigned to this ride");
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("ride-management-service", ex);
        }
    }

    /**
     * Marks the ride COMPLETED. The payment already exists in this service, so no amount is sent:
     * ride-management-service looks the payment up itself (with the driver's token) and refuses
     * to complete a ride that has not been paid through POST /api/payments/rides/{rideId}/{driverId}/Payment.
     */
    public void completeRide(String rideId, String callerAuthorizationHeader) {
        try {
            String url = baseUrl + "/api/rides/" + rideId + "/complete";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", callerAuthorizationHeader);
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(headers), RideDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RideNotFoundException(rideId);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new AccessDeniedException("You are not the driver assigned to this ride");
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("ride-management-service", ex);
        }
    }
}
