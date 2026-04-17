package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.ClassProgressionRequest;
import com.app.ark_backend_services.dto.ClassProgressionResponse;
import com.app.ark_backend_services.service.ClassProgressionService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/branches/{branchId}/class-progression")
@RequiredArgsConstructor
@Tag(name = "Class Progression", description = "Configure class ordering for a branch. Defines the sequence of classes (e.g., Grade 9 → Grade 10 → Grade 11 → Grade 12) and which class is terminal (graduation).")
public class ClassProgressionController {

    private final ClassProgressionService progressionService;

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Set class progression", description = "Creates or updates the class progression sequence for a branch. This is a prerequisite for student promotion.")
    @ApiResponse(responseCode = "200", description = "Class progression saved")
    @ApiResponse(responseCode = "400", description = "Empty sequence or invalid data")
    @ApiResponse(responseCode = "403", description = "Not ORG_ADMIN or SUPER_ADMIN")
    @ApiResponse(responseCode = "404", description = "Organization or branch not found")
    public ResponseEntity<ClassProgressionResponse> upsert(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Valid @RequestBody ClassProgressionRequest request) {
        return ResponseEntity.ok(progressionService.upsert(organizationId, branchId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Get class progression", description = "Returns the configured class progression for a branch. Returns 404 if not yet configured.")
    @ApiResponse(responseCode = "200", description = "Class progression found")
    @ApiResponse(responseCode = "404", description = "Class progression not configured for this branch")
    public ResponseEntity<ClassProgressionResponse> get(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId) {
        return ResponseEntity.ok(progressionService.get(organizationId, branchId));
    }
}
