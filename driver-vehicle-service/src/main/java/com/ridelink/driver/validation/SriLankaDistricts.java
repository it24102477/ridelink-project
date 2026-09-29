package com.ridelink.driver.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SriLankaDistricts {

    public static final List<String> ALL = List.of(
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo",
            "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara",
            "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar",
            "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya",
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya");

    private static final Map<String, String> BY_LOWER = new LinkedHashMap<>();
    static {
        ALL.forEach(d -> BY_LOWER.put(d.toLowerCase(), d));
    }

    private SriLankaDistricts() {}

    public static boolean isValid(String value) {
        return value != null && BY_LOWER.containsKey(value.trim().toLowerCase());
    }

    /** Returns the canonical spelling ("colombo " -> "Colombo"), or the input if unknown. */
    public static String canonical(String value) {
        if (value == null) return null;
        return BY_LOWER.getOrDefault(value.trim().toLowerCase(), value);
    }

    /**
     * Matches a raw district-ish string from a geocoder (e.g. "Colombo District", "gampaha")
     * to one of the 25 canonical district names, or returns null if it isn't one of them.
     */
    public static String match(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim();
        if (cleaned.toLowerCase().endsWith(" district")) {
            cleaned = cleaned.substring(0, cleaned.length() - " district".length()).trim();
        }
        return BY_LOWER.get(cleaned.toLowerCase());
    }
}