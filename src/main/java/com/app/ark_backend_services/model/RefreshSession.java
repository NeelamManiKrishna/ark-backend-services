package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One row per issued refresh token. Tokens are opaque random strings; only a SHA-256 hash is stored.
 * Each refresh rotates the token (old row marked {@code used}, new row in the same {@code family}).
 * Presenting a used or revoked token revokes the whole family (device session).
 */
@Data
@Document(collection = "refresh_sessions")
@CompoundIndex(name = "user_idx", def = "{'userId': 1, 'revokedAt': 1}")
public class RefreshSession {

    @Id
    private String id;

    private String userId;

    private String deviceId;

    private String deviceName;

    @Indexed(unique = true)
    private String tokenHash;

    /** Shared by every token rotated from the same login (one device session). */
    @Indexed
    private String family;

    private boolean used;

    private Instant createdAt;

    /** TTL index: MongoDB deletes the row once this instant passes. */
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private Instant revokedAt;
}
