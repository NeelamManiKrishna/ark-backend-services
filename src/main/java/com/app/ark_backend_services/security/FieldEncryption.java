package com.app.ark_backend_services.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for sensitive fields (government ID numbers). Stored form: {@code enc:v1:<base64(iv|ciphertext)>}.
 * Values without the prefix are treated as legacy plaintext on read, so data written before encryption was
 * enabled keeps working until the Phase 3 migration encrypts it.
 */
@Component
public class FieldEncryption {

    static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Mongo property converters are instantiated by Spring Data (no-arg), so the key is held statically.
    private static volatile SecretKeySpec key;

    private final String base64Key;

    public FieldEncryption(@Value("${app.encryption.field-key}") String base64Key) {
        this.base64Key = base64Key;
    }

    @PostConstruct
    void init() {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalStateException("app.encryption.field-key (FIELD_ENCRYPTION_KEY) must be a base64-encoded 32-byte key");
        }
        key = new SecretKeySpec(raw, "AES");
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct);
            return PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("Field encryption failed", e);
        }
    }

    public static String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored; // legacy plaintext
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Field decryption failed", e);
        }
    }

    /** Keeps the last four characters visible, e.g. {@code ******1234}. */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int visible = Math.min(4, value.length());
        return "*".repeat(Math.max(0, value.length() - visible)) + value.substring(value.length() - visible);
    }

    private static SecretKeySpec requireKey() {
        SecretKeySpec k = key;
        if (k == null) {
            throw new IllegalStateException("Field encryption key not initialised");
        }
        return k;
    }
}
