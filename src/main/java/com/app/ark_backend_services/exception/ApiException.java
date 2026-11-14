package com.app.ark_backend_services.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Exception carrying a stable error code (see Part A2 of the backend plan) and HTTP status. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS, "Invalid email or password");
    }

    public static ApiException tokenExpired(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.TOKEN_EXPIRED, message);
    }

    public static ApiException tokenRevoked(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.TOKEN_REVOKED, message);
    }

    public static ApiException accountLocked(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_LOCKED, message);
    }

    public static ApiException orgSuspended(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ORG_SUSPENDED, message);
    }
}
