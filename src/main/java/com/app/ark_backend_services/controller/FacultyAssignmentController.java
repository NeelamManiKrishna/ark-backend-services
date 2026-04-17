package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.service.FacultyAssignmentService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments")
@RequiredArgsConstructor
@Tag(name = "Faculty Assignments", description = "Manage faculty-to-class/subject assignments per academic year. Supports subject teachers, class teachers, or both.")
public class FacultyAssignmentController {

    private final FacultyAssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create faculty assignment", description = "Assigns a faculty member to a class/subject for a specific academic year.")
    @ApiResponse(responseCode = "201", description = "Assignment created")
    @ApiResponse(responseCode = "404", description = "Faculty or class not found")
    @ApiResponse(responseCode = "409", description = "Faculty already assigned to this class/subject for the academic year")
    public ResponseEntity<FacultyAssignmentResponse> create(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Valid @RequestBody CreateFacultyAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.create(organizationId, branchId, request));
    }

    @GetMapping("/{assignmentId}")
    @Operation(summary = "Get assignment by ID", description = "Returns a single faculty assignment.")
    @ApiResponse(responseCode = "200", description = "Assignment found")
    @ApiResponse(responseCode = "404", description = "Assignment not found")
    public ResponseEntity<FacultyAssignmentResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Assignment ID") @PathVariable String assignmentId) {
        return ResponseEntity.ok(assignmentService.getById(organizationId, assignmentId));
    }

    @GetMapping("/faculty/{facultyId}/active")
    @Operation(summary = "Get active assignments for faculty", description = "Returns all current active assignments for a faculty member.")
    @ApiResponse(responseCode = "200", description = "Active assignments retrieved")
    public ResponseEntity<List<FacultyAssignmentResponse>> getActiveByFaculty(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId) {
        return ResponseEntity.ok(assignmentService.getActiveByFaculty(organizationId, facultyId));
    }

    @GetMapping("/faculty/{facultyId}")
    @Operation(summary = "Get all assignments for faculty", description = "Returns all assignments (active and completed) for a faculty member, paginated.")
    @ApiResponse(responseCode = "200", description = "Assignments retrieved")
    public ResponseEntity<Page<FacultyAssignmentResponse>> getAllByFaculty(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(assignmentService.getAllByFaculty(organizationId, facultyId, pageable));
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "Get assignments by class", description = "Returns all faculty assignments for a specific class and academic year.")
    @ApiResponse(responseCode = "200", description = "Assignments retrieved")
    public ResponseEntity<Page<FacultyAssignmentResponse>> getByClassAndYear(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Class ID") @PathVariable String classId,
            @Parameter(description = "Academic year (e.g., 2025-2026)") @RequestParam String academicYear,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(assignmentService.getByClassAndYear(organizationId, classId, academicYear, pageable));
    }

    @GetMapping
    @Operation(summary = "Get assignments by branch and year", description = "Returns all faculty assignments for a branch in a specific academic year.")
    @ApiResponse(responseCode = "200", description = "Assignments retrieved")
    public ResponseEntity<Page<FacultyAssignmentResponse>> getByBranchAndYear(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Academic year (e.g., 2025-2026)") @RequestParam String academicYear,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(assignmentService.getByBranchAndYear(organizationId, branchId, academicYear, pageable));
    }

    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Update assignment", description = "Updates assignment type or status.")
    @ApiResponse(responseCode = "200", description = "Assignment updated")
    @ApiResponse(responseCode = "404", description = "Assignment not found")
    public ResponseEntity<FacultyAssignmentResponse> update(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Assignment ID") @PathVariable String assignmentId,
            @Valid @RequestBody UpdateFacultyAssignmentRequest request) {
        return ResponseEntity.ok(assignmentService.update(organizationId, assignmentId, request));
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete assignment", description = "Deletes a faculty assignment.")
    @ApiResponse(responseCode = "204", description = "Assignment deleted")
    @ApiResponse(responseCode = "404", description = "Assignment not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Assignment ID") @PathVariable String assignmentId) {
        assignmentService.delete(organizationId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
