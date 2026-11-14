package com.app.ark_backend_services.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Academic years are written YYYY-YYYY with consecutive years, e.g. 2025-2026. */
public final class AcademicYears {

    public static final String REGEX = "^\\d{4}-\\d{4}$";
    private static final Pattern PATTERN = Pattern.compile("^(\\d{4})-(\\d{4})$");

    private AcademicYears() {}

    /** Returns the starting year, or throws IllegalArgumentException when the format is wrong. */
    public static int startYear(String year) {
        Matcher m = year == null ? null : PATTERN.matcher(year);
        if (m == null || !m.matches() || Integer.parseInt(m.group(2)) != Integer.parseInt(m.group(1)) + 1) {
            throw new IllegalArgumentException(
                    "Invalid academic year '" + year + "', expected format YYYY-YYYY with consecutive years, e.g. 2025-2026");
        }
        return Integer.parseInt(m.group(1));
    }

    public static void validate(String year) {
        startYear(year);
    }

    /** The target year must come after the source year. */
    public static void assertLater(String target, String source) {
        if (startYear(target) <= startYear(source)) {
            throw new IllegalArgumentException(
                    "Target academic year " + target + " must be later than source year " + source);
        }
    }
}
