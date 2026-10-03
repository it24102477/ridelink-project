package com.ridelink.ride.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ridelink.ride.exception.LocationNotFoundException;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.validation.SriLankaDistricts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Address -> coordinates using Nominatim (OpenStreetMap), no API key needed.
 *
 * The public server allows about 1 request/second and requires an identifying User-Agent.
 * Results are cached so repeated addresses never hit it again. For no limit at all, run your
 * own Nominatim (or point ridelink.geocoding.nominatim-url at any compatible server).
 */
@Component
public class GeocodingClient {

    private static final int MAX_CACHE_ENTRIES = 5000;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String countryCodes;
    private final String userAgent;
    private final ConcurrentHashMap<String, Location> cache = new ConcurrentHashMap<>();

    public GeocodingClient(RestTemplate restTemplate,
                           @Value("${ridelink.geocoding.nominatim-url}") String baseUrl,
                           @Value("${ridelink.geocoding.country-codes:lk}") String countryCodes,
                           @Value("${ridelink.geocoding.user-agent}") String userAgent) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.countryCodes = countryCodes;
        this.userAgent = userAgent;
    }

    public Location geocode(String address) {
        String key = address.trim().toLowerCase(Locale.ROOT);
        Location cached = cache.get(key);
        if (cached != null) return copy(cached);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                .queryParam("q", address.trim())
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("countrycodes", countryCodes)
                .queryParam("addressdetails", 1)
                .build().toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        JsonNode body;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
        } catch (Exception e) {
            throw new DownstreamServiceException("geocoding service", e);
        }
        if (body == null || !body.isArray() || body.isEmpty()) throw new LocationNotFoundException(address);

        JsonNode hit = body.get(0);
        Location loc = new Location(hit.path("lat").asDouble(), hit.path("lon").asDouble(), address.trim());
        loc.setDistrict(resolveDistrict(hit.path("address")));
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
        cache.put(key, loc);
        return copy(loc);
    }

    /**
     * Nominatim's "address" breakdown names the district differently depending on the result
     * (state_district is the usual field for Sri Lanka; county/city_district are fallbacks for
     * some areas), so each candidate is tried in turn against the 25 known districts.
     */
    private String resolveDistrict(JsonNode addressNode) {
        for (String field : new String[]{"state_district", "county", "city_district", "region"}) {
            String candidate = addressNode.path(field).asText(null);
            String matched = SriLankaDistricts.match(candidate);
            if (matched != null) return matched;
        }
        return null;
    }

    private Location copy(Location l) {
        Location c = new Location(l.getLat(), l.getLng(), l.getAddress());
        c.setDistrict(l.getDistrict());
        return c;
    }
}
