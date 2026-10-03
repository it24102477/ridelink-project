package com.ridelink.ride.client;

import com.ridelink.ride.model.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Synchronous REST client to fare-payment-service, used both for the up-front
 * fare estimate on ride creation and the final fare + payment on ride completion.
 * Same justification as DriverServiceClient: the ride workflow cannot proceed
 * (and the passenger cannot be shown a fare) without an immediate response.
 */
@Component
public class FareServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public FareServiceClient(RestTemplate restTemplate,
                              @Value("${ridelink.services.fare-service-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public FareEstimateDto getEstimate(Location pickup, Location destination) {
        try {
            Map<String, Object> body = Map.of(
                    "pickupLat", pickup.getLat(), "pickupLng", pickup.getLng(),
                    "destinationLat", destination.getLat(), "destinationLng", destination.getLng()
            );
            FareEstimateDto result = restTemplate.postForObject(baseUrl + "/api/fares/estimate-coordinates", body, FareEstimateDto.class);
            if (result == null) throw new DownstreamServiceException("fare-payment-service", null);
            return result;
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("fare-payment-service", ex);
        }
    }

    public FinalFareDto getFinalFare(String rideId, Location pickup, Location destination, double actualDurationMinutes) {
        try {
            Map<String, Object> body = Map.of(
                    "rideId", rideId,
                    "pickupLat", pickup.getLat(), "pickupLng", pickup.getLng(),
                    "destinationLat", destination.getLat(), "destinationLng", destination.getLng(),
                    "actualDurationMinutes", actualDurationMinutes
            );
            FinalFareDto result = restTemplate.postForObject(baseUrl + "/api/fares/final", body, FinalFareDto.class);
            if (result == null) throw new DownstreamServiceException("fare-payment-service", null);
            return result;
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("fare-payment-service", ex);
        }
    }

    public PaymentResultDto recordPayment(String rideId, String passengerId, String driverUserId, double amount, PaymentMethodDto method) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("rideId", rideId);
            body.put("passengerId", passengerId);
            body.put("amount", amount);
            body.put("method", method.name());
            if (driverUserId != null) body.put("driverUserId", driverUserId);
            PaymentResultDto result = restTemplate.postForObject(baseUrl + "/api/payments", body, PaymentResultDto.class);
            if (result == null) throw new DownstreamServiceException("fare-payment-service", null);
            return result;
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("fare-payment-service", ex);
        }
    }

    /**
     * Looks up the driver's own payment for a ride, forwarding the driver's token. Returns null if
     * no payment exists yet. Used to make sure a ride is only completed after it has been paid
     * through the payment API, and to take the final fare from that payment.
     */
    public PaymentResultDto findDriverPaymentForRide(String driverUserId, String rideId, String authorizationHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorizationHeader);
            return restTemplate.exchange(baseUrl + "/api/driver/" + driverUserId + "/payments/ride/" + rideId,
                    HttpMethod.GET, new HttpEntity<>(headers), PaymentResultDto.class).getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (RestClientException ex) {
            throw new DownstreamServiceException("fare-payment-service", ex);
        }
    }
}
