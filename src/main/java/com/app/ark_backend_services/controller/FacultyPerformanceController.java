package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.service.FacultyPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/faculty/{facultyId}/performance")
@RequiredArgsConstructor
@Tag(name = "Faculty Performance", description = "Faculty performance metrics derived from exam results across assigned classes and subjects.")
public class FacultyPerformanceController {

    private final FacultyPerformanceService performanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Overall faculty performance", description = "Returns a comprehensive performance summary for a faculty member across all assigned classes and subjects.")
    @ApiResponse(responseCode = "200", description = "Performance data retrieved")
    @ApiResponse(responseCode = "404", description = "Faculty not found")
    public ResponseEntity<FacultyPerformanceResponse> getOverallPerformance(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId,
            @Parameter(description = "Academic year (e.g., 2025-2026)") @RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(performanceService.getOverallPerformance(organizationId, facultyId, academicYear));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Faculty performance in a class", description = "Returns performance metrics for a faculty member in a specific class, including exam-wise breakdown.")
    @ApiResponse(responseCode = "200", description = "Performance data retrieved")
    @ApiResponse(responseCode = "404", description = "Faculty or class not found, or not assigned")
    public ResponseEntity<FacultyClassPerformanceResponse> getClassPerformance(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId,
            @Parameter(description = "Class ID") @PathVariable String classId,
            @Parameter(description = "Academic year (e.g., 2025-2026)") @RequestParam String academicYear) {
        return ResponseEntity.ok(performanceService.getClassPerformance(organizationId, facultyId, classId, academicYear));
    }

    @GetMapping("/subject/{subjectName}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Faculty performance across classes for a subject", description = "Compares faculty performance teaching the same subject across different classes.")
    @ApiResponse(responseCode = "200", description = "Performance data retrieved")
    @ApiResponse(responseCode = "404", description = "Faculty not found")
    public ResponseEntity<FacultySubjectPerformanceResponse> getSubjectPerformance(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Faculty ID") @PathVariable String facultyId,
            @Parameter(description = "Subject name (e.g., Mathematics)") @PathVariable String subjectName,
            @Parameter(description = "Academic year (e.g., 2025-2026)") @RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(performanceService.getSubjectPerformance(organizationId, facultyId, subjectName, academicYear));
    }
}
