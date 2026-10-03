package com.ridelink.fare.service;

import com.ridelink.fare.client.GeocodingClient;
import com.ridelink.fare.client.RoutingClient;
import com.ridelink.fare.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FareService {

    private final FareCalculator calculator;
    private final RoutingClient routingClient;
    private final GeocodingClient geocodingClient;
    private final String currency;

    public FareService(FareCalculator calculator, RoutingClient routingClient, GeocodingClient geocodingClient,
                       @Value("${ridelink.fare.currency}") String currency) {
        this.calculator = calculator;
        this.routingClient = routingClient;
        this.geocodingClient = geocodingClient;
        this.currency = currency;
    }

    /** Real road distance from OSRM when available, otherwise straight-line Haversine. */
    private double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        return routingClient.route(lat1, lng1, lat2, lng2)
                .map(RoutingClient.Route::distanceKm)
                .orElseGet(() -> calculator.distanceKm(lat1, lng1, lat2, lng2));
    }

    /** Two addresses in -> coordinates, road distance and estimated fare out. */
    public AddressFareEstimateResponse estimateByAddress(FareEstimateRequest req) {
        var from = geocodingClient.geocode(req.getPickup());
        var to = geocodingClient.geocode(req.getDestination());
        FareEstimateResponse e = estimate(new FareEstimateByCoordinatesRequest(from.lat(), from.lng(), to.lat(), to.lng()));
        return new AddressFareEstimateResponse(
                new AddressFareEstimateResponse.Point(from.address(), from.lat(), from.lng()),
                new AddressFareEstimateResponse.Point(to.address(), to.lat(), to.lng()),
                e.getDistanceKm(), e.getEstimatedFare(), e.getCurrency());
    }

    /** Server-to-server: used by ride-management-service, which has already geocoded. */
    public FareEstimateResponse estimate(FareEstimateByCoordinatesRequest req) {
        var route = routingClient.route(req.getPickupLat(), req.getPickupLng(),
                req.getDestinationLat(), req.getDestinationLng());
        double distance = route.map(RoutingClient.Route::distanceKm).orElseGet(() ->
                calculator.distanceKm(req.getPickupLat(), req.getPickupLng(),
                        req.getDestinationLat(), req.getDestinationLng()));
        double duration = route.map(RoutingClient.Route::durationMinutes)
                .orElseGet(() -> calculator.estimatedDurationMinutes(distance));
        double fare = calculator.calculateFare(distance, duration);
        return new FareEstimateResponse(round2(distance), fare, currency);
    }

    /** Called by ride-management-service when a ride is completed. */
    public FinalFareResponse finalFare(FinalFareRequest req) {
        double distance = distanceKm(req.getPickupLat(), req.getPickupLng(),
                req.getDestinationLat(), req.getDestinationLng());
        double fare = calculator.calculateFare(distance, req.getActualDurationMinutes());
        return new FinalFareResponse(req.getRideId(), round2(distance), req.getActualDurationMinutes(), fare, currency);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
