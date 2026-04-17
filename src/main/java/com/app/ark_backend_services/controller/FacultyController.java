package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.CreateFacultyRequest;
import com.app.ark_backend_services.dto.FacultyResponse;
import com.app.ark_backend_services.dto.UpdateFacultyRequest;
import com.app.ark_backend_services.service.FacultyService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/faculty")
@RequiredArgsConstructor
@Tag(name = "Faculty", description = "Manage faculty members within an organization. SUPER_ADMIN, ORG_ADMIN, and ADMIN can create/update/delete.")
public class FacultyController {

    private final FacultyService facultyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create faculty", description = "Creates a new faculty member in the organization. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Faculty created")
    @ApiResponse(responseCode = "404", description = "Organization or branch not found")
    @ApiResponse(responseCode = "409", description = "Employee ID already exists")
    public ResponseEntity<FacultyResponse> create(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Valid @RequestBody CreateFacultyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facultyService.create(organizationId, request));
    }

    @GetMapping("/{facultyId}")
    @Operation(summary = "Get faculty by ID", description = "Returns a single faculty member scoped to the organization.")
    @ApiResponse(responseCode = "200", description = "Faculty found")
    @ApiResponse(responseCode = "404", description = "Faculty not found")
    public ResponseEntity<FacultyResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId) {
        return ResponseEntity.ok(facultyService.getById(organizationId, facultyId));
    }

    @GetMapping
    @Operation(summary = "List faculty", description = "Returns a paginated list of faculty. Optionally filter by branchId or department query parameter.")
    @ApiResponse(responseCode = "200", description = "Faculty retrieved")
    public ResponseEntity<Page<FacultyResponse>> getAll(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Filter by branch ID") @RequestParam(required = false) String branchId,
            @Parameter(description = "Filter by department") @RequestParam(required = false) String department,
            @SortDefault(sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        if (branchId != null) {
            return ResponseEntity.ok(facultyService.getAllByBranch(organizationId, branchId, pageable));
        }
        if (department != null) {
            return ResponseEntity.ok(facultyService.getAllByDepartment(organizationId, department, pageable));
        }
        return ResponseEntity.ok(facultyService.getAllByOrganization(organizationId, pageable));
    }

    @PutMapping("/{facultyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Update faculty", description = "Partially updates a faculty record. Only non-null fields are applied.")
    @ApiResponse(responseCode = "200", description = "Faculty updated")
    @ApiResponse(responseCode = "404", description = "Faculty not found")
    @ApiResponse(responseCode = "409", description = "Employee ID already exists")
    public ResponseEntity<FacultyResponse> update(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId,
            @Valid @RequestBody UpdateFacultyRequest request) {
        return ResponseEntity.ok(facultyService.update(organizationId, facultyId, request));
    }

    @DeleteMapping("/{facultyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete faculty", description = "Deletes a faculty member. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Faculty deleted")
    @ApiResponse(responseCode = "404", description = "Faculty not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId) {
        facultyService.delete(organizationId, facultyId);
        return ResponseEntity.noContent().build();
    }
}
