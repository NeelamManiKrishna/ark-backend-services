package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.AuditLogResponse;
import com.app.ark_backend_services.model.AuditLog;
import com.app.ark_backend_services.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "View platform audit trail. SUPER_ADMIN only.")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List audit logs", description = "Returns a paginated list of audit logs. Optionally filter by organizationId, action, entityType, or userId. Requires SUPER_ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Audit logs retrieved")
    public ResponseEntity<Page<AuditLogResponse>> getAll(
            @Parameter(description = "Filter by organization ID") @RequestParam(required = false) String organizationId,
            @Parameter(description = "Filter by action (CREATE, UPDATE, DELETE, LOGIN, REGISTER)") @RequestParam(required = false) AuditLog.Action action,
            @Parameter(description = "Filter by entity type (Organization, Branch, Student, etc.)") @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by user ID who performed the action") @RequestParam(required = false) String userId,
            @SortDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLogResponse> result;

        if (organizationId != null) {
            result = auditLogService.getByOrganization(organizationId, pageable);
        } else if (action != null) {
            result = auditLogService.getByAction(action, pageable);
        } else if (entityType != null) {
            result = auditLogService.getByEntityType(entityType, pageable);
        } else if (userId != null) {
            result = auditLogService.getByUser(userId, pageable);
        } else {
            result = auditLogService.getAll(pageable);
        }

        return ResponseEntity.ok(result);
    }
}
