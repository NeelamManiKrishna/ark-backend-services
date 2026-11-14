package com.app.ark_backend_services.util;

import java.util.Locale;
import java.util.Set;

/** Gender is stored as MALE, FEMALE or OTHER; input is accepted case-insensitively. */
public final class Genders {

    private static final Set<String> ALLOWED = Set.of("MALE", "FEMALE", "OTHER");

    private Genders() {}

    /** Returns the normalized value, null for blank input, or throws IllegalArgumentException for anything else. */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(v)) {
            throw new IllegalArgumentException("Invalid gender '" + value + "', expected MALE, FEMALE or OTHER");
        }
        return v;
    }

    /** Like {@link #normalize} but returns null instead of throwing (used by migrations). */
    public static String normalizeOrNull(String value) {
        try {
            return normalize(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
