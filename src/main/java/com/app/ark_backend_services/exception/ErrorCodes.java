package com.app.ark_backend_services.exception;

/** Stable machine-readable error codes returned in the {@code error} field of every error body. */
public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_REVOKED = "TOKEN_REVOKED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ORG_SUSPENDED = "ORG_SUSPENDED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String CONFLICT = "CONFLICT";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
