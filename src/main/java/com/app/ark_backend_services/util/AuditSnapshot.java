package com.app.ark_backend_services.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Captures the simple, non-sensitive fields of an entity so audit entries can store before/after values. */
public final class AuditSnapshot {

    /** Never copied into audit logs. */
    private static final Set<String> EXCLUDED = Set.of(
            "password", "govtIdNumber", "version", "createdAt", "updatedAt", "tokenVersion",
            "failedAttempts", "lockedUntil");

    private AuditSnapshot() {}

    public static Map<String, Object> of(Object entity) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (entity == null) {
            return out;
        }
        for (Field f : entity.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || EXCLUDED.contains(f.getName())) {
                continue;
            }
            Class<?> t = f.getType();
            boolean simple = t.isPrimitive() || t == String.class || Number.class.isAssignableFrom(t)
                    || t == Boolean.class || t.isEnum() || TemporalAccessor.class.isAssignableFrom(t);
            if (!simple) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object v = f.get(entity);
                out.put(f.getName(), (v == null || t.isPrimitive() || v instanceof Number || v instanceof Boolean)
                        ? v : v.toString());
            } catch (IllegalAccessException ignored) {
                // skip unreadable field
            }
        }
        return out;
    }

    /** Names of fields whose value differs between the two snapshots. */
    public static List<String> changedFields(Map<String, Object> before, Map<String, Object> after) {
        return after.keySet().stream()
                .filter(k -> !java.util.Objects.equals(before.get(k), after.get(k)))
                .toList();
    }
}
