package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.AuthResponse;
import com.app.ark_backend_services.dto.ChangePasswordRequest;
import com.app.ark_backend_services.dto.LoginRequest;
import com.app.ark_backend_services.dto.LogoutRequest;
import com.app.ark_backend_services.dto.MeResponse;
import com.app.ark_backend_services.dto.RefreshTokenRequest;
import com.app.ark_backend_services.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh/rotation, logout and session endpoints. Accounts are created by admins only.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login", description = "Authenticates with email and password. Returns an access token and a rotating refresh token. Send deviceId for a longer-lived device session.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS")
    @ApiResponse(responseCode = "403", description = "ACCOUNT_LOCKED or ORG_SUSPENDED")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh tokens", description = "Exchanges a refresh token for a new pair. Each refresh token is single-use; reusing one revokes the whole device session.")
    @ApiResponse(responseCode = "200", description = "New token pair")
    @ApiResponse(responseCode = "401", description = "TOKEN_EXPIRED or TOKEN_REVOKED")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Logout this device", description = "Revokes the device session that owns the refresh token. Always returns 204.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all devices", description = "Revokes every device session of the current user and invalidates issued access tokens.")
    @ApiResponse(responseCode = "204", description = "Logged out everywhere")
    public ResponseEntity<Void> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Current user and access scope", description = "Returns the user and their scope (role, organizationId, branchIds, studentIds).")
    public ResponseEntity<MeResponse> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Revokes all other sessions and returns a fresh token pair for this device.")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Current password incorrect or new password unchanged")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(request));
    }
}
