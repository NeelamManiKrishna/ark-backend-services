package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.CreateUserRequest;
import com.app.ark_backend_services.dto.UpdateUserRequest;
import com.app.ark_backend_services.dto.UserResponse;
import com.app.ark_backend_services.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Manage user accounts within an organization. Access varies by role.")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create user", description = "Creates a new user in the organization. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid request or branchId missing for ADMIN/USER roles")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    public ResponseEntity<UserResponse> create(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(organizationId, request));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Get user by ID", description = "Returns a single user scoped to the organization. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "User ID") @PathVariable String userId) {
        return ResponseEntity.ok(userService.getById(organizationId, userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "List users", description = "Returns a paginated list of users in the organization. Optionally filter by branchId.")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    public ResponseEntity<Page<UserResponse>> getAll(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Filter by branch ID") @RequestParam(required = false) String branchId,
            @SortDefault(sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(organizationId, branchId, pageable));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Update user", description = "Partially updates a user. Only non-null fields are applied. Requires SUPER_ADMIN or ORG_ADMIN role.")
    @ApiResponse(responseCode = "200", description = "User updated")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "User ID") @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(organizationId, userId, request));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Delete user", description = "Deletes a user account. Requires SUPER_ADMIN or ORG_ADMIN role.")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "User ID") @PathVariable String userId) {
        userService.delete(organizationId, userId);
        return ResponseEntity.noContent().build();
    }
}
