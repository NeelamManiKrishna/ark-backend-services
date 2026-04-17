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
