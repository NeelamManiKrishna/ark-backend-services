package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/branches/{branchId}/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Preview and execute student promotions from one class to the next. Requires class progression to be configured first.")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Preview promotion", description = "Returns a list of students with promote/hold-back/graduate recommendations based on FINAL exam results. Does not make any changes.")
    @ApiResponse(responseCode = "200", description = "Preview generated")
    @ApiResponse(responseCode = "400", description = "Class progression not configured or source class not in progression")
    @ApiResponse(responseCode = "404", description = "Source class not found")
    public ResponseEntity<PromotionPreviewResponse> preview(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Source class ID to promote FROM") @RequestParam String sourceClassId,
            @Parameter(description = "Target academic year (e.g., 2026-2027)") @RequestParam String targetAcademicYear) {
        return ResponseEntity.ok(promotionService.preview(organizationId, branchId, sourceClassId, targetAcademicYear));
    }

    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Execute promotion", description = "Promotes students from source class to the next class. Closes old enrollments, creates new enrollments, auto-creates target class if needed, and marks source class as COMPLETED.")
    @ApiResponse(responseCode = "200", description = "Promotion executed successfully")
    @ApiResponse(responseCode = "400", description = "Class progression not configured or source class not in progression")
    @ApiResponse(responseCode = "404", description = "Source class or student not found")
    public ResponseEntity<PromotionExecuteResponse> execute(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Valid @RequestBody PromotionExecuteRequest request) {
        return ResponseEntity.ok(promotionService.execute(organizationId, branchId, request));
    }
}
