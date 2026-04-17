package com.app.ark_backend_services.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "audit_logs")
@CompoundIndex(name = "org_timestamp_idx", def = "{'organizationId': 1, 'timestamp': -1}")
@CompoundIndex(name = "entity_idx", def = "{'entityType': 1, 'entityId': 1}")
public class AuditLog {

    @Id
    private String id;

    private Action action;

    private String entityType;

    private String entityId;

    private String entityName;

    private String organizationId;

    private String performedBy;

    private String performedByEmail;

    private String performedByRole;

    private String details;

    @Indexed
    private Instant timestamp;

    public enum Action {
        CREATE, UPDATE, DELETE, LOGIN, LOGIN_FAILED, LOGOUT, REGISTER
    }
}
