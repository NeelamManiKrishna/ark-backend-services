package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.CreateUserRequest;
import com.app.ark_backend_services.dto.UpdateUserRequest;
import com.app.ark_backend_services.dto.UserResponse;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.UserRepository;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserResponse create(String organizationId, CreateUserRequest request) {
        validateOrgAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        // ADMIN and USER must be mapped to a branch
        if (request.getRole() == User.Role.ADMIN || request.getRole() == User.Role.USER) {
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

        // Enforce role creation hierarchy
        validateRoleCreation(request.getRole());

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
        validateOrgAccess(organizationId);

        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return UserResponse.from(user);
    }

    public Page<UserResponse> getAll(String organizationId, String branchId, Pageable pageable) {
        validateOrgAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }

        Page<User> users;
        if (branchId != null && !branchId.isBlank()) {
            users = userRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable);
        } else {
            users = userRepository.findByOrganizationId(organizationId, pageable);
        }

        return users.map(UserResponse::from);
    }

    public UserResponse update(String organizationId, String userId, UpdateUserRequest request) {
        validateOrgAccess(organizationId);

        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Enforce role update hierarchy
        if (request.getRole() != null) {
            validateRoleCreation(request.getRole());
        }

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getBranchId() != null) user.setBranchId(request.getBranchId());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        // After all fields are set, validate branchId requirement for ADMIN/USER
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.USER) {
            if (user.getBranchId() == null || user.getBranchId().isBlank()) {
                throw new IllegalArgumentException("Branch ID is required for ADMIN and USER roles");
            }
            branchRepository.findByIdAndOrganizationId(user.getBranchId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + user.getBranchId()));
        }

        UserResponse response = UserResponse.from(userRepository.save(user));
        auditLogService.log(Action.UPDATE, "User", userId, user.getFullName(), organizationId, "User updated");
        return response;
    }

    public void delete(String organizationId, String userId) {
        validateOrgAccess(organizationId);

        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        auditLogService.log(Action.DELETE, "User", userId, user.getFullName(), organizationId, "User deleted");
        userRepository.delete(user);
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
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
}
