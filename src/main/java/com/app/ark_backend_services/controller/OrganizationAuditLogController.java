package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.AuditLogResponse;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit trail of a single organization (SUPER_ADMIN or that organization's ORG_ADMIN).")
public class OrganizationAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "List an organization's audit logs", description = "Paginated, newest first.")
    public ResponseEntity<Page<AuditLogResponse>> getForOrganization(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Filter by action") @org.springframework.web.bind.annotation.RequestParam(required = false) com.app.ark_backend_services.model.AuditLog.Action action,
            @Parameter(description = "Filter by entity type") @org.springframework.web.bind.annotation.RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by user ID") @org.springframework.web.bind.annotation.RequestParam(required = false) String userId,
            @SortDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        return ResponseEntity.ok(auditLogService.search(organizationId, action, entityType, userId, pageable));
    }
}
