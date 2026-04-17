package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.AuthResponse;
import com.app.ark_backend_services.dto.LoginRequest;
import com.app.ark_backend_services.dto.RefreshTokenRequest;
import com.app.ark_backend_services.dto.RegisterRequest;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.UserRepository;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    public AuthResponse register(RegisterRequest request) {
        // Only authenticated SUPER_ADMINs can register SUPER_ADMIN or ORG_ADMIN accounts
        if (request.getRole() == User.Role.SUPER_ADMIN || request.getRole() == User.Role.ORG_ADMIN) {
            if (!CurrentUser.isSuperAdmin()) {
                throw new AccessDeniedException("Only Super Admins can register " + request.getRole() + " accounts");
            }
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        // Non-SUPER_ADMIN users must belong to an organization
        if (request.getRole() != User.Role.SUPER_ADMIN) {
            if (request.getOrganizationId() == null || request.getOrganizationId().isBlank()) {
                throw new IllegalArgumentException("Organization ID is required for non-Super Admin users");
            }
            if (!organizationRepository.existsById(request.getOrganizationId())) {
                throw new ResourceNotFoundException("Organization not found: " + request.getOrganizationId());
            }

            // ADMIN and USER must be mapped to a branch
            if (request.getRole() == User.Role.ADMIN || request.getRole() == User.Role.USER) {
                if (request.getBranchId() == null || request.getBranchId().isBlank()) {
                    throw new IllegalArgumentException("Branch ID is required for ADMIN and USER roles");
                }
                branchRepository.findByIdAndOrganizationId(request.getBranchId(), request.getOrganizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));
            } else if (request.getBranchId() != null && !request.getBranchId().isBlank()) {
                // ORG_ADMIN with optional branch — validate if provided
                branchRepository.findByIdAndOrganizationId(request.getBranchId(), request.getOrganizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));
            }
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setOrganizationId(request.getOrganizationId());
        user.setBranchId(request.getBranchId());
        user.setDepartment(request.getDepartment());
        user.setStatus(User.UserStatus.ACTIVE);

        user = userRepository.save(user);

        auditLogService.logAuth(Action.REGISTER, user.getId(), user.getEmail(),
                user.getRole().name(), user.getOrganizationId(),
                "User registered: " + user.getFullName());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            log.warn("Login failed: unknown email {}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: wrong password for {}", request.getEmail());
            auditLogService.logAuth(Action.LOGIN_FAILED, user.getId(), user.getEmail(),
                    user.getRole().name(), user.getOrganizationId(),
                    "Failed login attempt: incorrect password");
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login failed: account {} is {}", request.getEmail(), user.getStatus());
            auditLogService.logAuth(Action.LOGIN_FAILED, user.getId(), user.getEmail(),
                    user.getRole().name(), user.getOrganizationId(),
                    "Failed login attempt: account " + user.getStatus().name().toLowerCase());
            throw new IllegalArgumentException("Account is " + user.getStatus().name().toLowerCase());
        }

        auditLogService.logAuth(Action.LOGIN, user.getId(), user.getEmail(),
                user.getRole().name(), user.getOrganizationId(),
                "User logged in");

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtUtil.isValid(token)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        if (!"refresh".equals(jwtUtil.getTokenType(token))) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        String userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is " + user.getStatus().name().toLowerCase());
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user))
                .refreshToken(jwtUtil.generateRefreshToken(user))
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .organizationId(user.getOrganizationId())
                        .build())
                .build();
    }
}
