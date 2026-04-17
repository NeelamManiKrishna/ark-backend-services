package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.BranchDashboardResponse;
import com.app.ark_backend_services.dto.OrgDashboardResponse;
import com.app.ark_backend_services.dto.PlatformDashboardResponse;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Analytics and metrics endpoints for platform, organization, and branch levels.")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/platform")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Platform dashboard", description = "Returns platform-wide metrics: total orgs, branches, students, faculty, users. Requires SUPER_ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Platform metrics retrieved")
    public ResponseEntity<PlatformDashboardResponse> getPlatformDashboard() {
        return ResponseEntity.ok(dashboardService.getPlatformDashboard());
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Organization dashboard", description = "Returns organization-level metrics: branches, students, faculty, users, exams. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Organization metrics retrieved")
    @ApiResponse(responseCode = "403", description = "No access to this organization")
    @ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<OrgDashboardResponse> getOrgDashboard(
            @Parameter(description = "Organization ID") @PathVariable String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
        return ResponseEntity.ok(dashboardService.getOrgDashboard(organizationId));
    }

    @GetMapping("/organization/{organizationId}/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Branch dashboard", description = "Returns branch-level metrics: classes, students, faculty, exams. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Branch metrics retrieved")
    @ApiResponse(responseCode = "403", description = "No access to this organization")
    @ApiResponse(responseCode = "404", description = "Organization or branch not found")
    public ResponseEntity<BranchDashboardResponse> getBranchDashboard(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
        return ResponseEntity.ok(dashboardService.getBranchDashboard(organizationId, branchId));
    }
}
