package com.app.ark_backend_services.security;

import com.app.ark_backend_services.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and parses short-lived access tokens. Claims: sub (userId), role, org, branch,
 * ver (user token version), typ ("access"). Refresh tokens are opaque and handled by AuthService.
 */
@Component
public class JwtUtil {

    public static final String TYPE_ACCESS = "access";

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration:1800000}") long accessTokenExpirationMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.getId())
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .claim("typ", TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(key);
        if (user.getOrganizationId() != null) {
            builder.claim("org", user.getOrganizationId());
        }
        if (user.getBranchId() != null) {
            builder.claim("branch", user.getBranchId());
        }
        return builder.compact();
    }

    /** Parses and verifies signature and expiry. Throws JwtException (ExpiredJwtException when expired). */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}
