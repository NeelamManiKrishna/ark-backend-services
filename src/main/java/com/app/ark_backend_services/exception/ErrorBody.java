package com.app.ark_backend_services.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the stable error body: {error, message, timestamp, path, fieldErrors?}. */
public final class ErrorBody {
    private ErrorBody() {}

    public static Map<String, Object> of(String code, String message, String path) {
        return of(code, message, path, null);
    }

    public static Map<String, Object> of(String code, String message, String path,
                                          List<Map<String, String>> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        body.put("path", path);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }
}
