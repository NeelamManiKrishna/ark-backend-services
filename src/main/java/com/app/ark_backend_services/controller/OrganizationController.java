package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.CreateOrganizationRequest;
import com.app.ark_backend_services.dto.OrganizationResponse;
import com.app.ark_backend_services.dto.UpdateOrganizationRequest;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.service.OrganizationService;
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
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Manage organizations (tenants) on the platform. SUPER_ADMIN only for create/update/delete.")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create organization", description = "Creates a new organization. Requires SUPER_ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Organization created")
    @ApiResponse(responseCode = "409", description = "Organization name already exists")
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID", description = "Returns a single organization by its ID. Non-SUPER_ADMIN users can only view their own organization.")
    @ApiResponse(responseCode = "200", description = "Organization found")
    @ApiResponse(responseCode = "403", description = "Access denied — not your organization")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<OrganizationResponse> getById(@Parameter(description = "Organization ID") @PathVariable String id) {
        if (!CurrentUser.isSuperAdmin() && !CurrentUser.belongsToOrg(id)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only view your own organization");
        }
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all organizations", description = "Returns a paginated list of all organizations. Requires SUPER_ADMIN role. Non-SUPER_ADMIN users should use GET /organizations/{id} with their own org ID.")
    @ApiResponse(responseCode = "200", description = "Organizations retrieved")
    public ResponseEntity<Page<OrganizationResponse>> getAll(
            @SortDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(organizationService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update organization", description = "Partially updates an organization. Only non-null fields are applied. Requires SUPER_ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Organization updated")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    @ApiResponse(responseCode = "409", description = "Organization name already exists")
    public ResponseEntity<OrganizationResponse> update(@Parameter(description = "Organization ID") @PathVariable String id,
                                                        @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete organization", description = "Deletes an organization and all related data (users, branches, classes, students, faculty, exams). Requires SUPER_ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Organization deleted")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<Void> delete(@Parameter(description = "Organization ID") @PathVariable String id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
