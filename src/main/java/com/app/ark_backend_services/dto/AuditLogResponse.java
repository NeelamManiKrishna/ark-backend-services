package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuditLogResponse {

    private String id;
    private AuditLog.Action action;
    private String entityType;
    private String entityId;
    private String entityName;
    private String organizationId;
    private String performedBy;
    private String performedByEmail;
    private String performedByRole;
    private String details;
    private Instant timestamp;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityName(log.getEntityName())
                .organizationId(log.getOrganizationId())
                .performedBy(log.getPerformedBy())
                .performedByEmail(log.getPerformedByEmail())
                .performedByRole(log.getPerformedByRole())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}
