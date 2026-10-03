package com.ridelink.fare.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Road distance / duration from an OSRM server (OpenStreetMap data, no API key).
 *
 * The default base URL is the public demo server, which is fine for development but has a
 * fair-use policy. For no usage limit at all, run your own OSRM (see docker-compose) and set
 * ridelink.routing.osrm-url to it. Any failure returns Optional.empty() so the caller can
 * fall back to the Haversine formula - a map outage never blocks a ride.
 */
@Component
public class RoutingClient {

    private static final Logger log = LoggerFactory.getLogger(RoutingClient.class);
    private static final int MAX_CACHE_ENTRIES = 5000;

    public record Route(double distanceKm, double durationMinutes) {}

    private final RestTemplate restTemplate;
    private final String osrmUrl;
    private final boolean enabled;
    private final ConcurrentHashMap<String, Route> cache = new ConcurrentHashMap<>();

    public RoutingClient(RestTemplate restTemplate,
                         @Value("${ridelink.routing.osrm-url}") String osrmUrl,
                         @Value("${ridelink.routing.enabled:true}") boolean enabled) {
        this.restTemplate = restTemplate;
        this.osrmUrl = osrmUrl.endsWith("/") ? osrmUrl.substring(0, osrmUrl.length() - 1) : osrmUrl;
        this.enabled = enabled;
    }

    public Optional<Route> route(double lat1, double lng1, double lat2, double lng2) {
        if (!enabled) return Optional.empty();
        // ~11 m precision is plenty for fares and makes repeat lookups free
        String key = String.format(Locale.ROOT, "%.4f,%.4f;%.4f,%.4f", lat1, lng1, lat2, lng2);
        Route cached = cache.get(key);
        if (cached != null) return Optional.of(cached);
        try {
            // OSRM expects lng,lat order
            String url = String.format(Locale.ROOT,
                    "%s/route/v1/driving/%f,%f;%f,%f?overview=false", osrmUrl, lng1, lat1, lng2, lat2);
            JsonNode body = restTemplate.getForObject(url, JsonNode.class);
            if (body == null || !"Ok".equals(body.path("code").asText())) return Optional.empty();
            JsonNode r = body.path("routes").path(0);
            if (r.isMissingNode()) return Optional.empty();
            Route route = new Route(r.path("distance").asDouble() / 1000.0,
                                    r.path("duration").asDouble() / 60.0);
            if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
            cache.put(key, route);
            return Optional.of(route);
        } catch (Exception e) {
            log.warn("OSRM lookup failed, falling back to Haversine: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
