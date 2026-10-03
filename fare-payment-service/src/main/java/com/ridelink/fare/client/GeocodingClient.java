package com.ridelink.fare.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ridelink.fare.exception.LocationNotFoundException;
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

    public record GeocodedLocation(double lat, double lng, String address) {}


    private static final int MAX_CACHE_ENTRIES = 5000;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String countryCodes;
    private final String userAgent;
    private final ConcurrentHashMap<String, GeocodedLocation> cache = new ConcurrentHashMap<>();

    public GeocodingClient(RestTemplate restTemplate,
                           @Value("${ridelink.geocoding.nominatim-url}") String baseUrl,
                           @Value("${ridelink.geocoding.country-codes:lk}") String countryCodes,
                           @Value("${ridelink.geocoding.user-agent}") String userAgent) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.countryCodes = countryCodes;
        this.userAgent = userAgent;
    }

    public GeocodedLocation geocode(String address) {
        String key = address.trim().toLowerCase(Locale.ROOT);
        GeocodedLocation cached = cache.get(key);
        if (cached != null) return copy(cached);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                .queryParam("q", address.trim())
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("countrycodes", countryCodes)
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
        GeocodedLocation loc = new GeocodedLocation(hit.path("lat").asDouble(), hit.path("lon").asDouble(), address.trim());
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
        cache.put(key, loc);
        return copy(loc);
    }

    private GeocodedLocation copy(GeocodedLocation l) { return l; }
}
