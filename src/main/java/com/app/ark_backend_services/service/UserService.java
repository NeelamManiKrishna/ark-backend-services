package com.app.ark_backend_services.service;

import com.app.ark_backend_services.util.AuditSnapshot;
import com.app.ark_backend_services.dto.CreateUserRequest;
import com.app.ark_backend_services.dto.UpdateUserRequest;
import com.app.ark_backend_services.dto.UserResponse;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.RefreshSessionRepository;
import com.app.ark_backend_services.repository.UserRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final RefreshSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SearchService searchService;

    public UserResponse create(String organizationId, CreateUserRequest request) {
        AccessPolicy.assertOrg(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }

        // Enforce role creation hierarchy
        validateRoleCreation(request.getRole());

        // A branch-scoped ADMIN can only create users inside their own branch
        if (AccessPolicy.isBranchScoped()) {
            AccessPolicy.assertBranch(request.getBranchId());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        // ADMIN and USER must be mapped to a branch
        if (request.getRole().isBranchScoped()) {
            if (request.getBranchId() == null || request.getBranchId().isBlank()) {
                throw new IllegalArgumentException("Branch ID is required for ADMIN and USER roles");
            }
            branchRepository.findByIdAndOrganizationId(request.getBranchId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));
        } else if (request.getBranchId() != null && !request.getBranchId().isBlank()) {
            // ORG_ADMIN with optional branch — validate if provided
            branchRepository.findByIdAndOrganizationId(request.getBranchId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setOrganizationId(organizationId);
        user.setBranchId(request.getBranchId());
        user.setDepartment(request.getDepartment());
        user.setStatus(User.UserStatus.ACTIVE);

        UserResponse response = UserResponse.from(userRepository.save(user));
        auditLogService.log(Action.CREATE, "User", response.getId(), user.getFullName(), organizationId, "User created with role " + user.getRole());
        return response;
    }

    public UserResponse getById(String organizationId, String userId) {
        AccessPolicy.assertOrg(organizationId);

        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        assertVisible(user);

        return UserResponse.from(user);
    }

    public Page<UserResponse> getAll(String organizationId, String branchId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }

        String effectiveBranch = AccessPolicy.restrictBranch(
                branchId == null || branchId.isBlank() ? null : branchId);

        Page<User> users;
        if (AccessPolicy.isBranchScoped()) {
            // ADMIN never sees ORG_ADMIN/SUPER_ADMIN accounts
            users = userRepository.findByOrganizationIdAndBranchIdAndRoleIn(organizationId, effectiveBranch,
                    List.of(User.Role.ADMIN, User.Role.USER), pageable);
        } else if (effectiveBranch != null) {
            users = userRepository.findByOrganizationIdAndBranchId(organizationId, effectiveBranch, pageable);
        } else {
            users = userRepository.findByOrganizationId(organizationId, pageable);
        }

        return users.map(UserResponse::from);
    }

    public UserResponse update(String organizationId, String userId, UpdateUserRequest request) {
        AccessPolicy.assertOrg(organizationId);

        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        var auditBefore = AuditSnapshot.of(user);
        assertCanManage(user);

        // Enforce role update hierarchy
        if (request.getRole() != null) {
            validateRoleCreation(request.getRole());
        }

        boolean sessionAffecting = false;

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            sessionAffecting = true;
        }
        if (request.getRole() != null && request.getRole() != user.getRole()) {
            if (user.getRole() == User.Role.ORG_ADMIN) {
                assertNotLastOrgAdmin(user);
            }
            user.setRole(request.getRole());
            sessionAffecting = true;
        }
        if (request.getBranchId() != null && !request.getBranchId().equals(user.getBranchId())) {
            branchRepository.findByIdAndOrganizationId(request.getBranchId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));
            user.setBranchId(request.getBranchId());
            sessionAffecting = true;
        }
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getStatus() != null && request.getStatus() != user.getStatus()) {
            if (request.getStatus() != User.UserStatus.ACTIVE && user.getRole() == User.Role.ORG_ADMIN) {
                assertNotLastOrgAdmin(user);
            }
            user.setStatus(request.getStatus());
            if (request.getStatus() == User.UserStatus.ACTIVE) {
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
            } else {
                sessionAffecting = true;
            }
        }

        // After all fields are set, validate branchId requirement for ADMIN/USER
        if (user.getRole().isBranchScoped()) {
            if (user.getBranchId() == null || user.getBranchId().isBlank()) {
                throw new IllegalArgumentException("Branch ID is required for ADMIN and USER roles");
            }
            branchRepository.findByIdAndOrganizationId(user.getBranchId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + user.getBranchId()));
        }

        if (sessionAffecting) {
            // Existing access tokens carry the old version; refresh tokens are dropped
            user.setTokenVersion(user.getTokenVersion() + 1);
            sessionRepository.deleteByUserId(user.getId());
        }

        UserResponse response = UserResponse.from(userRepository.save(user));
        auditLogService.logChange(Action.UPDATE, "User", userId, user.getFullName(), organizationId, "User updated", auditBefore, AuditSnapshot.of(user));
        return response;
    }

    public void delete(String organizationId, String userId) {
        AccessPolicy.assertOrg(organizationId);

        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        assertCanManage(user);
        if (user.getRole() == User.Role.ORG_ADMIN) {
            assertNotLastOrgAdmin(user);
        }

        auditLogService.log(Action.DELETE, "User", userId, user.getFullName(), organizationId, "User deleted");
        sessionRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    // ---- rules ----

    /** Higher number = more privileged. Unknown/future roles rank lowest. */
    private static int rank(User.Role role) {
        return switch (role) {
            case SUPER_ADMIN -> 3;
            case ORG_ADMIN -> 2;
            case ADMIN -> 1;
            case USER -> 0;
        };
    }

    /** Branch-scoped callers only see ADMIN/USER accounts of their own branch. */
    private void assertVisible(User target) {
        if (AccessPolicy.isBranchScoped()) {
            if (!target.getRole().isBranchScoped()) {
                throw new AccessDeniedException("You do not have access to this user");
            }
            AccessPolicy.assertBranch(target.getBranchId());
        }
    }

    /** Callers may only modify accounts strictly below their own role, never themselves. */
    private void assertCanManage(User target) {
        User caller = CurrentUser.get();
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (caller.getId().equals(target.getId())) {
            throw new AccessDeniedException("You cannot modify or delete your own account here");
        }
        if (rank(caller.getRole()) <= rank(target.getRole())) {
            throw new AccessDeniedException("You cannot modify a user with role " + target.getRole());
        }
        assertVisible(target);
    }

    private void assertNotLastOrgAdmin(User target) {
        if (userRepository.countByOrganizationIdAndRole(target.getOrganizationId(), User.Role.ORG_ADMIN) <= 1) {
            throw new IllegalStateException("Cannot remove, demote or deactivate the last ORG_ADMIN of an organization");
        }
    }

    private void validateRoleCreation(User.Role targetRole) {
        User currentUser = CurrentUser.get();
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        // SUPER_ADMIN can create any role
        if (currentUser.getRole() == User.Role.SUPER_ADMIN) return;

        // ORG_ADMIN can create ADMIN and USER
        if (currentUser.getRole() == User.Role.ORG_ADMIN) {
            if (targetRole == User.Role.SUPER_ADMIN || targetRole == User.Role.ORG_ADMIN) {
                throw new AccessDeniedException("You cannot create users with role " + targetRole);
            }
            return;
        }

        // ADMIN can create USER only
        if (currentUser.getRole() == User.Role.ADMIN) {
            if (targetRole != User.Role.USER) {
                throw new AccessDeniedException("You can only create users with USER role");
            }
            return;
        }

        // USER cannot create other users
        throw new AccessDeniedException("You do not have permission to create users");
    }

    /** Prefix search ("q") over name and email, within the caller's scope (ADMIN only sees ADMIN/USER of their branch). */
    public Page<UserResponse> search(String organizationId, String branchId, String q, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        String effectiveBranch = AccessPolicy.restrictBranch(branchId == null || branchId.isBlank() ? null : branchId);
        org.springframework.data.mongodb.core.query.Criteria scope = org.springframework.data.mongodb.core.query.Criteria.where("organizationId").is(organizationId);
        if (effectiveBranch != null) {
            scope = scope.and("branchId").is(effectiveBranch);
        }
        if (AccessPolicy.isBranchScoped()) {
            scope = scope.and("role").in(User.Role.ADMIN, User.Role.USER);
        }
        return searchService.search(User.class, scope, q, java.util.List.of("fullName", "email"), pageable, UserResponse::from);
    }
}
