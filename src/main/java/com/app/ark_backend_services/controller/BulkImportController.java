package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.BulkImportResponse;
import com.app.ark_backend_services.service.BulkImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/bulk-import")
@RequiredArgsConstructor
@Tag(name = "Bulk Import", description = "CSV bulk import for students, faculty, classes, and branches")
public class BulkImportController {

    private final BulkImportService bulkImportService;

    @PostMapping(value = "/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk import students", description = "Upload a CSV file to create multiple students. Auto-creates enrollments if classId and academicYear are provided.")
    @ApiResponse(responseCode = "200", description = "Import completed — check successCount and errors")
    @ApiResponse(responseCode = "400", description = "Invalid or empty CSV file")
    public ResponseEntity<BulkImportResponse> importStudents(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "CSV file") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bulkImportService.importStudents(organizationId, file));
    }

    @PostMapping(value = "/faculty", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk import faculty", description = "Upload a CSV file to create multiple faculty members. Use pipe (|) delimiter for qualifications and specializations.")
    @ApiResponse(responseCode = "200", description = "Import completed — check successCount and errors")
    @ApiResponse(responseCode = "400", description = "Invalid or empty CSV file")
    public ResponseEntity<BulkImportResponse> importFaculty(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "CSV file") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bulkImportService.importFaculty(organizationId, file));
    }

    @PostMapping(value = "/academic-classes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk import academic classes", description = "Upload a CSV file to create multiple academic classes. Skips duplicates matching {orgId, branchId, name, section, academicYear}.")
    @ApiResponse(responseCode = "200", description = "Import completed — check successCount and errors")
    @ApiResponse(responseCode = "400", description = "Invalid or empty CSV file")
    public ResponseEntity<BulkImportResponse> importAcademicClasses(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "CSV file") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bulkImportService.importAcademicClasses(organizationId, file));
    }

    @PostMapping(value = "/branches", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Bulk import branches", description = "Upload a CSV file to create multiple branches. Skips duplicates by name within the organization.")
    @ApiResponse(responseCode = "200", description = "Import completed — check successCount and errors")
    @ApiResponse(responseCode = "400", description = "Invalid or empty CSV file")
    public ResponseEntity<BulkImportResponse> importBranches(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "CSV file") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bulkImportService.importBranches(organizationId, file));
    }

    @GetMapping("/samples/{entityType}")
    @Operation(summary = "Download sample CSV", description = "Download a sample CSV template for the specified entity type. Valid types: students, faculty, academic-classes, branches")
    @ApiResponse(responseCode = "200", description = "Sample CSV returned")
    public ResponseEntity<byte[]> downloadSample(
            @Parameter(description = "Entity type: students, faculty, academic-classes, branches") @PathVariable String entityType) {
        String csv = bulkImportService.getSampleCsv(entityType);
        String filename = "sample_" + entityType.replace("-", "_") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
