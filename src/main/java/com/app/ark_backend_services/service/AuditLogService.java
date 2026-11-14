package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.AuditLogResponse;
import com.app.ark_backend_services.model.AuditLog;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.repository.AuditLogRepository;
import com.app.ark_backend_services.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public void log(Action action, String entityType, String entityId, String entityName,
                    String organizationId, String details) {
        User currentUser = CurrentUser.get();

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .organizationId(organizationId)
                .performedBy(currentUser != null ? currentUser.getId() : null)
                .performedByEmail(currentUser != null ? currentUser.getEmail() : null)
                .performedByRole(currentUser != null ? currentUser.getRole().name() : null)
                .details(details)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    /** UPDATE entry that also stores what changed. Call after the write has succeeded. */
    public void logChange(Action action, String entityType, String entityId, String entityName,
                          String organizationId, String details,
                          java.util.Map<String, Object> before, java.util.Map<String, Object> after) {
        User currentUser = CurrentUser.get();
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .organizationId(organizationId)
                .performedBy(currentUser != null ? currentUser.getId() : null)
                .performedByEmail(currentUser != null ? currentUser.getEmail() : null)
                .performedByRole(currentUser != null ? currentUser.getRole().name() : null)
                .details(details)
                .before(before)
                .after(after)
                .changedFields(com.app.ark_backend_services.util.AuditSnapshot.changedFields(before, after))
                .timestamp(Instant.now())
                .build());
    }

    /** Counts rows per collection matching the query, e.g. "students=120, faculty=8". Used before cascade deletes. */
    public String countCascade(org.springframework.data.mongodb.core.query.Query query, String... collections) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String collection : collections) {
            long n = mongoTemplate.count(query, collection);
            if (n > 0) {
                parts.add(collection + "=" + n);
            }
        }
        return parts.isEmpty() ? "no child records" : String.join(", ", parts);
    }

    /** All filters combine (AND); any of them may be null. */
    public Page<AuditLogResponse> search(String organizationId, Action action, String entityType, String userId,
                                         Pageable pageable) {
        org.springframework.data.mongodb.core.query.Criteria c = new org.springframework.data.mongodb.core.query.Criteria();
        java.util.List<org.springframework.data.mongodb.core.query.Criteria> parts = new java.util.ArrayList<>();
        if (organizationId != null) parts.add(org.springframework.data.mongodb.core.query.Criteria.where("organizationId").is(organizationId));
        if (action != null) parts.add(org.springframework.data.mongodb.core.query.Criteria.where("action").is(action));
        if (entityType != null) parts.add(org.springframework.data.mongodb.core.query.Criteria.where("entityType").is(entityType));
        if (userId != null) parts.add(org.springframework.data.mongodb.core.query.Criteria.where("performedBy").is(userId));
        if (!parts.isEmpty()) {
            c = c.andOperator(parts.toArray(new org.springframework.data.mongodb.core.query.Criteria[0]));
        }
        org.springframework.data.mongodb.core.query.Query query = org.springframework.data.mongodb.core.query.Query.query(c);
        long total = mongoTemplate.count(query, AuditLog.class);
        java.util.List<AuditLog> rows = mongoTemplate.find(query.with(pageable), AuditLog.class);
        return new org.springframework.data.domain.PageImpl<>(rows.stream().map(AuditLogResponse::from).toList(), pageable, total);
    }

    public void logAuth(Action action, String userId, String email, String role,
                        String organizationId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType("User")
                .entityId(userId)
                .entityName(email)
                .organizationId(organizationId)
                .performedBy(userId)
                .performedByEmail(email)
                .performedByRole(role)
                .details(details)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    public Page<AuditLogResponse> getAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> getByOrganization(String organizationId, Pageable pageable) {
        return auditLogRepository.findByOrganizationId(organizationId, pageable)
                .map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> getByAction(AuditLog.Action action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable)
                .map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> getByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable)
                .map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> getByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByPerformedBy(userId, pageable)
                .map(AuditLogResponse::from);
    }
}
