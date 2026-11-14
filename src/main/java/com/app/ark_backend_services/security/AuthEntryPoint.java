package com.app.ark_backend_services.security;

import com.app.ark_backend_services.exception.ErrorBody;
import com.app.ark_backend_services.exception.ErrorCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes the stable error body for unauthenticated requests. {@link JwtAuthenticationFilter} stores the
 * precise reason (TOKEN_EXPIRED, TOKEN_REVOKED, ORG_SUSPENDED, ACCOUNT_LOCKED) in a request attribute.
 */
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

    public static final String ERROR_CODE_ATTRIBUTE = "ark.auth.error";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "ark.auth.message";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = (String) request.getAttribute(ERROR_CODE_ATTRIBUTE);
        String message = (String) request.getAttribute(ERROR_MESSAGE_ATTRIBUTE);
        if (code == null) {
            code = ErrorCodes.UNAUTHORIZED;
            message = "Authentication is required to access this resource";
        }
        boolean forbidden = ErrorCodes.ORG_SUSPENDED.equals(code) || ErrorCodes.ACCOUNT_LOCKED.equals(code);

        response.setStatus(forbidden ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorBody.of(code, message, request.getRequestURI()));
    }
}
