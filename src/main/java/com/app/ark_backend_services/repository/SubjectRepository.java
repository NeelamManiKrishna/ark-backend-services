package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SubjectRepository extends MongoRepository<Subject, String> {

    Page<Subject> findByOrganizationId(String organizationId, Pageable pageable);

    Optional<Subject> findByIdAndOrganizationId(String id, String organizationId);

    Optional<Subject> findFirstByOrganizationIdAndNameIgnoreCase(String organizationId, String name);

    boolean existsByOrganizationIdAndCode(String organizationId, String code);

    void deleteByOrganizationId(String organizationId);
}
