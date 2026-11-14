package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Enrollment summary and exam performance. JSON by default, CSV with ?format=csv. Branch-scoped roles only see their own branch.")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/enrollment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Enrollment summary", description = "Enrollment counts per branch and class for an academic year.")
    public ResponseEntity<?> enrollment(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Academic year, e.g. 2025-2026") @RequestParam String academicYear,
            @Parameter(description = "Limit to one branch") @RequestParam(required = false) String branchId,
            @Parameter(description = "json (default) or csv") @RequestParam(defaultValue = "json") String format) {
        return respond(reportService.enrollmentSummary(organizationId, branchId, academicYear), format, "enrollment-summary");
    }

    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Exam performance", description = "Pass rate, average, highest and lowest marks per exam subject for an academic year.")
    public ResponseEntity<?> performance(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Academic year, e.g. 2025-2026") @RequestParam String academicYear,
            @Parameter(description = "Limit to one branch") @RequestParam(required = false) String branchId,
            @Parameter(description = "json (default) or csv") @RequestParam(defaultValue = "json") String format) {
        return respond(reportService.examPerformance(organizationId, branchId, academicYear), format, "exam-performance");
    }

    private ResponseEntity<?> respond(ReportService.Report report, String format, String name) {
        if ("csv".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + name + "-" + report.academicYear() + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(ReportService.toCsv(report).getBytes(StandardCharsets.UTF_8));
        }
        if (!"json".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("format must be json or csv");
        }
        return ResponseEntity.ok(report);
    }
}
