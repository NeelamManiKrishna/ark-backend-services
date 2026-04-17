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

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration:3600000}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiration, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiration, "refresh");
    }

    private String buildToken(User user, long expiration, String tokenType) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(key);

        if (user.getOrganizationId() != null) {
            builder.claim("organizationId", user.getOrganizationId());
        }

        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String getOrganizationId(String token) {
        return parseToken(token).get("organizationId", String.class);
    }

    public String getTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }
}
