package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByOrganizationId(String organizationId, Pageable pageable);

    Page<AuditLog> findByAction(AuditLog.Action action, Pageable pageable);

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndAction(String organizationId, AuditLog.Action action, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndEntityType(String organizationId, String entityType, Pageable pageable);

    long countByAction(AuditLog.Action action);

    long countByOrganizationIdAndAction(String organizationId, AuditLog.Action action);

    java.util.List<AuditLog> findTop10ByOrderByTimestampDesc();

    java.util.List<AuditLog> findTop10ByOrganizationIdOrderByTimestampDesc(String organizationId);
}
