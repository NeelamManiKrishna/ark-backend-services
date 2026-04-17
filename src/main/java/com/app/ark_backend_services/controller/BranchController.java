package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.BranchResponse;
import com.app.ark_backend_services.dto.CreateBranchRequest;
import com.app.ark_backend_services.dto.UpdateBranchRequest;
import com.app.ark_backend_services.service.BranchService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Manage branches within an organization. SUPER_ADMIN and ORG_ADMIN can create/update/delete.")
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Create branch", description = "Creates a new branch under the specified organization. Requires SUPER_ADMIN or ORG_ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Branch created")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    @ApiResponse(responseCode = "409", description = "Branch name already exists in this organization")
    public ResponseEntity<BranchResponse> create(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(organizationId, request));
    }

    @GetMapping("/{branchId}")
    @Operation(summary = "Get branch by ID", description = "Returns a single branch scoped to the organization.")
    @ApiResponse(responseCode = "200", description = "Branch found")
    @ApiResponse(responseCode = "404", description = "Branch not found")
    public ResponseEntity<BranchResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId) {
        return ResponseEntity.ok(branchService.getById(organizationId, branchId));
    }

    @GetMapping
    @Operation(summary = "List branches by organization", description = "Returns a paginated list of all branches in the organization.")
    @ApiResponse(responseCode = "200", description = "Branches retrieved")
    public ResponseEntity<Page<BranchResponse>> getAll(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @SortDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(branchService.getAllByOrganization(organizationId, pageable));
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Update branch", description = "Partially updates a branch. Only non-null fields are applied. Requires SUPER_ADMIN or ORG_ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Branch updated")
    @ApiResponse(responseCode = "404", description = "Branch not found")
    @ApiResponse(responseCode = "409", description = "Branch name already exists in this organization")
    public ResponseEntity<BranchResponse> update(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Valid @RequestBody UpdateBranchRequest request) {
        return ResponseEntity.ok(branchService.update(organizationId, branchId, request));
    }

    @DeleteMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Delete branch", description = "Deletes a branch and all related classes, examinations, and exam data. Requires SUPER_ADMIN or ORG_ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Branch deleted")
    @ApiResponse(responseCode = "404", description = "Branch not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId) {
        branchService.delete(organizationId, branchId);
        return ResponseEntity.noContent().build();
    }
}
