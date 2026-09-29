package com.ridelink.driver.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ridelink.driver.exception.DownstreamServiceException;
import com.ridelink.driver.exception.LocationNotFoundException;
import com.ridelink.driver.model.GeoPoint;
import com.ridelink.driver.validation.SriLankaDistricts;
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
 * Address -> coordinates using Nominatim (OpenStreetMap): free, no API key. Same client the
 * ride and fare services use. The public server allows about 1 request/second and requires an
 * identifying User-Agent, so results are cached; point ridelink.geocoding.nominatim-url at your
 * own Nominatim if you need no limit at all.
 */
@Component
public class GeocodingClient {

    private static final int MAX_CACHE_ENTRIES = 5000;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String countryCodes;
    private final String userAgent;
    private final ConcurrentHashMap<String, GeoPoint> cache = new ConcurrentHashMap<>();

    public GeocodingClient(RestTemplate restTemplate,
                           @Value("${ridelink.geocoding.nominatim-url}") String baseUrl,
                           @Value("${ridelink.geocoding.country-codes:lk}") String countryCodes,
                           @Value("${ridelink.geocoding.user-agent}") String userAgent) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.countryCodes = countryCodes;
        this.userAgent = userAgent;
    }

    public GeoPoint geocode(String address) {
        String key = address.trim().toLowerCase(Locale.ROOT);
        GeoPoint cached = cache.get(key);
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
        GeoPoint point = new GeoPoint(address.trim(), hit.path("lat").asDouble(), hit.path("lon").asDouble());
        point.setDistrict(resolveDistrict(hit.path("address")));
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
        cache.put(key, point);
        return copy(point);
    }

    /** Nominatim names the district differently per result, so each candidate field is tried in turn. */
    private String resolveDistrict(JsonNode addressNode) {
        for (String field : new String[]{"state_district", "county", "city_district", "region"}) {
            String matched = SriLankaDistricts.match(addressNode.path(field).asText(null));
            if (matched != null) return matched;
        }
        return null;
    }

    private GeoPoint copy(GeoPoint p) {
        GeoPoint c = new GeoPoint(p.getAddress(), p.getLat(), p.getLng());
        c.setDistrict(p.getDistrict());
        return c;
    }
}
