package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.AuthResponse;
import com.app.ark_backend_services.dto.ChangePasswordRequest;
import com.app.ark_backend_services.dto.LoginRequest;
import com.app.ark_backend_services.dto.MeResponse;
import com.app.ark_backend_services.dto.RefreshTokenRequest;
import com.app.ark_backend_services.exception.ApiException;
import com.app.ark_backend_services.exception.ErrorCodes;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.Branch;
import com.app.ark_backend_services.model.Organization;
import com.app.ark_backend_services.model.RefreshSession;
import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.RefreshSessionRepository;
import com.app.ark_backend_services.repository.UserRepository;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Compared against when the email is unknown so response time does not reveal which emails exist. */
    private static final String DUMMY_HASH = "$2a$10$0V4TSH2bEuUh/x6ctUj1teDES0mNyyfhYd9i6Mb5SHC0tuBF1qmJW";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final RefreshSessionRepository sessionRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.refresh-token-expiration:604800000}")
    private long webRefreshTtlMs;

    @Value("${app.jwt.device-refresh-token-expiration:2592000000}")
    private long deviceRefreshTtlMs;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-minutes:15}")
    private long lockoutMinutes;

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH);
            log.warn("Login failed: unknown email");
            auditLogService.logAuth(Action.LOGIN_FAILED, null, email, null, null,
                    "Failed login attempt: unknown email");
            throw ApiException.invalidCredentials();
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            auditLogService.logAuth(Action.LOGIN_FAILED, user.getId(), user.getEmail(), user.getRole().name(),
                    user.getOrganizationId(), "Failed login attempt: account temporarily locked");
            throw ApiException.accountLocked("Too many failed attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            registerFailedAttempt(user);
            log.warn("Login failed: wrong password for {}", email);
            auditLogService.logAuth(Action.LOGIN_FAILED, user.getId(), user.getEmail(), user.getRole().name(),
                    user.getOrganizationId(), "Failed login attempt: incorrect password");
            throw ApiException.invalidCredentials();
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login failed: account {} is {}", email, user.getStatus());
            auditLogService.logAuth(Action.LOGIN_FAILED, user.getId(), user.getEmail(), user.getRole().name(),
                    user.getOrganizationId(), "Failed login attempt: account " + user.getStatus().name().toLowerCase());
            throw ApiException.accountLocked("Account is " + user.getStatus().name().toLowerCase());
        }

        assertOrgAndBranchActive(user);

        if (user.getFailedAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        auditLogService.logAuth(Action.LOGIN, user.getId(), user.getEmail(), user.getRole().name(),
                user.getOrganizationId(), "User logged in");

        return issueSession(user, request.getDeviceId(), request.getDeviceName(), UUID.randomUUID().toString());
    }

    /** Rotating refresh: each refresh token works once; reuse revokes the whole device session. */
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshSession session = sessionRepository.findByTokenHash(hash(request.getRefreshToken()))
                .orElseThrow(() -> ApiException.tokenRevoked("Invalid refresh token"));

        if (session.getRevokedAt() != null) {
            revokeFamily(session.getFamily());
            throw ApiException.tokenRevoked("Session has been revoked");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.tokenExpired("Refresh token has expired");
        }

        // Atomically mark as used; if someone already did, this is a reuse of a rotated token.
        RefreshSession claimed = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(session.getId()).and("used").is(false)),
                new Update().set("used", true), RefreshSession.class);
        if (claimed == null) {
            revokeFamily(session.getFamily());
            log.warn("Refresh token reuse detected for user {}", session.getUserId());
            throw ApiException.tokenRevoked("Refresh token reuse detected; session revoked");
        }

        User user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null || user.getStatus() != User.UserStatus.ACTIVE
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now()))) {
            revokeFamily(session.getFamily());
            throw ApiException.tokenRevoked("Account is not active");
        }
        try {
            assertOrgAndBranchActive(user);
        } catch (ApiException e) {
            revokeFamily(session.getFamily());
            throw e;
        }

        return issueSession(user, session.getDeviceId(), session.getDeviceName(), session.getFamily());
    }

    public void logout(String refreshToken) {
        sessionRepository.findByTokenHash(hash(refreshToken)).ifPresent(session -> {
            revokeFamily(session.getFamily());
            userRepository.findById(session.getUserId()).ifPresent(u ->
                    auditLogService.logAuth(Action.LOGOUT, u.getId(), u.getEmail(), u.getRole().name(),
                            u.getOrganizationId(), "User logged out"));
        });
    }

    /** Revokes every device session and invalidates outstanding access tokens. */
    public void logoutAll() {
        User user = requireCurrentUser();
        revokeAllSessions(user.getId());
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditLogService.logAuth(Action.LOGOUT, user.getId(), user.getEmail(), user.getRole().name(),
                user.getOrganizationId(), "User logged out of all devices");
    }

    public MeResponse me() {
        return MeResponse.from(requireCurrentUser());
    }

    /** Success revokes every session, then issues a fresh one so the current device stays signed in. */
    public AuthResponse changePassword(ChangePasswordRequest request) {
        User user = userRepository.findById(requireCurrentUser().getId())
                .orElseThrow(ApiException::invalidCredentials);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Current password is incorrect");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "New password must differ from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        revokeAllSessions(user.getId());

        auditLogService.log(Action.UPDATE, "User", user.getId(), user.getEmail(), user.getOrganizationId(),
                "Password changed");

        return issueSession(user, request.getDeviceId(), request.getDeviceName(), UUID.randomUUID().toString());
    }

    // ---- helpers ----

    private User requireCurrentUser() {
        User user = CurrentUser.get();
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Authentication is required");
        }
        return user;
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(Instant.now().plusSeconds(lockoutMinutes * 60));
            user.setFailedAttempts(0);
        } else {
            user.setFailedAttempts(attempts);
        }
        userRepository.save(user);
    }

    private void assertOrgAndBranchActive(User user) {
        if (user.getOrganizationId() == null) {
            return;
        }
        Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        if (org == null || org.getStatus() != Organization.OrganizationStatus.ACTIVE) {
            throw ApiException.orgSuspended("Your organization is not active");
        }
        if (user.getRole().isBranchScoped() && user.getBranchId() != null) {
            Branch branch = branchRepository.findByIdAndOrganizationId(user.getBranchId(), user.getOrganizationId())
                    .orElse(null);
            if (branch == null || branch.getStatus() != Branch.BranchStatus.ACTIVE) {
                throw ApiException.orgSuspended("Your branch is not active");
            }
        }
    }

    private AuthResponse issueSession(User user, String deviceId, String deviceName, String family) {
        String rawRefreshToken = randomToken();
        boolean device = deviceId != null && !deviceId.isBlank();
        Instant now = Instant.now();

        RefreshSession session = new RefreshSession();
        session.setUserId(user.getId());
        session.setDeviceId(device ? deviceId : null);
        session.setDeviceName(deviceName);
        session.setTokenHash(hash(rawRefreshToken));
        session.setFamily(family);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusMillis(device ? deviceRefreshTtlMs : webRefreshTtlMs));
        sessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user))
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenTtlSeconds())
                .user(AuthResponse.UserInfo.from(user))
                .build();
    }

    private void revokeFamily(String family) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("family").is(family).and("revokedAt").is(null)),
                new Update().set("revokedAt", Instant.now()), RefreshSession.class);
    }

    private void revokeAllSessions(String userId) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("userId").is(userId).and("revokedAt").is(null)),
                new Update().set("revokedAt", Instant.now()), RefreshSession.class);
    }

    private static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
