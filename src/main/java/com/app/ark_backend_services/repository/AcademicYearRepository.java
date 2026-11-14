package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.AcademicYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends MongoRepository<AcademicYear, String> {

    Page<AcademicYear> findByOrganizationId(String organizationId, Pageable pageable);

    Optional<AcademicYear> findByIdAndOrganizationId(String id, String organizationId);

    Optional<AcademicYear> findByOrganizationIdAndLabel(String organizationId, String label);

    boolean existsByOrganizationIdAndLabel(String organizationId, String label);

    List<AcademicYear> findByOrganizationIdAndCurrentTrue(String organizationId);

    void deleteByOrganizationId(String organizationId);
}
