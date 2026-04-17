package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.service.StudentEnrollmentService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments")
@RequiredArgsConstructor
@Tag(name = "Student Enrollments", description = "Manage student class enrollments per academic year. Tracks enrollment history and supports promotion workflows.")
public class StudentEnrollmentController {

    private final StudentEnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Enroll student in a class", description = "Creates an active enrollment for a student in a class for a specific academic year. A student can only have one active enrollment at a time.")
    @ApiResponse(responseCode = "201", description = "Enrollment created")
    @ApiResponse(responseCode = "404", description = "Student or class not found")
    @ApiResponse(responseCode = "409", description = "Student already has an active enrollment or already enrolled for this academic year")
    public ResponseEntity<StudentEnrollmentResponse> enroll(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Valid @RequestBody CreateEnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(organizationId, branchId, request));
    }

    @GetMapping("/student/{studentId}/active")
    @Operation(summary = "Get active enrollment", description = "Returns the current active enrollment for a student.")
    @ApiResponse(responseCode = "200", description = "Active enrollment found")
    @ApiResponse(responseCode = "404", description = "No active enrollment found")
    public ResponseEntity<StudentEnrollmentResponse> getActiveEnrollment(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Student ID") @PathVariable String studentId) {
        return ResponseEntity.ok(enrollmentService.getActiveEnrollment(organizationId, studentId));
    }

    @GetMapping("/student/{studentId}/history")
    @Operation(summary = "Get enrollment history", description = "Returns all enrollments for a student across all academic years, ordered by most recent first.")
    @ApiResponse(responseCode = "200", description = "Enrollment history retrieved")
    public ResponseEntity<List<StudentEnrollmentResponse>> getEnrollmentHistory(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Student ID") @PathVariable String studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentHistory(organizationId, studentId));
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "Get enrollments by class", description = "Returns all active enrollments for a specific class (i.e., the current class roster).")
    @ApiResponse(responseCode = "200", description = "Enrollments retrieved")
    public ResponseEntity<Page<StudentEnrollmentResponse>> getEnrollmentsByClass(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Class ID") @PathVariable String classId,
            @SortDefault(sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByClass(organizationId, classId, pageable));
    }

    @GetMapping
    @Operation(summary = "Get enrollments by branch and year", description = "Returns all enrollments for a branch in a specific academic year.")
    @ApiResponse(responseCode = "200", description = "Enrollments retrieved")
    public ResponseEntity<Page<StudentEnrollmentResponse>> getEnrollmentsByBranchAndYear(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Academic year (e.g., 2025-2026)") @RequestParam String academicYear,
            @SortDefault(sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByBranchAndYear(organizationId, branchId, academicYear, pageable));
    }

    @PutMapping("/{enrollmentId}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Close enrollment", description = "Marks an enrollment as COMPLETED with an exit reason (PROMOTED, GRADUATED, HELD_BACK, TRANSFERRED, DROPPED).")
    @ApiResponse(responseCode = "200", description = "Enrollment closed")
    @ApiResponse(responseCode = "404", description = "Enrollment not found")
    @ApiResponse(responseCode = "400", description = "Enrollment already closed")
    public ResponseEntity<StudentEnrollmentResponse> closeEnrollment(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Enrollment ID") @PathVariable String enrollmentId,
            @Valid @RequestBody CloseEnrollmentRequest request) {
        return ResponseEntity.ok(enrollmentService.closeEnrollment(organizationId, enrollmentId, request));
    }
}
