package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.AcademicClassResponse;
import com.app.ark_backend_services.dto.CreateAcademicClassRequest;
import com.app.ark_backend_services.dto.UpdateAcademicClassRequest;
import com.app.ark_backend_services.service.AcademicClassService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/branches/{branchId}/classes")
@RequiredArgsConstructor
@Tag(name = "Academic Classes", description = "Manage classes within a branch. SUPER_ADMIN, ORG_ADMIN, and ADMIN can create/update/delete.")
public class AcademicClassController {

    private final AcademicClassService academicClassService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create class", description = "Creates a new academic class under the specified branch. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Class created")
    @ApiResponse(responseCode = "404", description = "Organization or branch not found")
    @ApiResponse(responseCode = "409", description = "Class with same name, section, and academic year already exists")
    public ResponseEntity<AcademicClassResponse> create(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Valid @RequestBody CreateAcademicClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicClassService.create(organizationId, branchId, request));
    }

    @GetMapping("/{classId}")
    @Operation(summary = "Get class by ID", description = "Returns a single academic class scoped to the branch.")
    @ApiResponse(responseCode = "200", description = "Class found")
    @ApiResponse(responseCode = "404", description = "Class not found")
    public ResponseEntity<AcademicClassResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Class ID") @PathVariable String classId) {
        return ResponseEntity.ok(academicClassService.getById(organizationId, branchId, classId));
    }

    @GetMapping
    @Operation(summary = "List classes by branch", description = "Returns a paginated list of all classes in the branch.")
    @ApiResponse(responseCode = "200", description = "Classes retrieved")
    public ResponseEntity<Page<AcademicClassResponse>> getAll(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @SortDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(academicClassService.getAllByBranch(organizationId, branchId, pageable));
    }

    @PutMapping("/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Update class", description = "Partially updates an academic class. Only non-null fields are applied.")
    @ApiResponse(responseCode = "200", description = "Class updated")
    @ApiResponse(responseCode = "404", description = "Class not found")
    @ApiResponse(responseCode = "409", description = "Class with same name, section, and academic year already exists")
    public ResponseEntity<AcademicClassResponse> update(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Class ID") @PathVariable String classId,
            @Valid @RequestBody UpdateAcademicClassRequest request) {
        return ResponseEntity.ok(academicClassService.update(organizationId, branchId, classId, request));
    }

    @DeleteMapping("/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete class", description = "Deletes an academic class. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Class deleted")
    @ApiResponse(responseCode = "404", description = "Class not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Class ID") @PathVariable String classId) {
        academicClassService.delete(organizationId, branchId, classId);
        return ResponseEntity.noContent().build();
    }
}
