package com.ridelink.ride.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The 25 districts of Sri Lanka, used to canonicalise a district name resolved by the geocoder. */
public final class SriLankaDistricts {

    public static final List<String> ALL = List.of(
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo",
            "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara",
            "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar",
            "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya",
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya");

    private static final Map<String, String> BY_LOWER = new LinkedHashMap<>();
    static {
        ALL.forEach(d -> BY_LOWER.put(d.toLowerCase(Locale.ROOT), d));
    }

    private SriLankaDistricts() {}

    /**
     * Matches a raw district-ish string from a geocoder (e.g. "Colombo District", "gampaha")
     * to one of the 25 canonical district names, or returns null if it isn't one of them.
     */
    public static String match(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim();
        if (cleaned.toLowerCase(Locale.ROOT).endsWith(" district")) {
            cleaned = cleaned.substring(0, cleaned.length() - " district".length()).trim();
        }
        return BY_LOWER.get(cleaned.toLowerCase(Locale.ROOT));
    }
}
