package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.Examination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ExaminationRepository extends MongoRepository<Examination, String> {

    Optional<Examination> findByIdAndOrganizationId(String id, String organizationId);

    Page<Examination> findByOrganizationIdAndBranchId(String organizationId, String branchId, Pageable pageable);

    Page<Examination> findByOrganizationIdAndBranchIdAndAcademicYear(String organizationId, String branchId, String academicYear, Pageable pageable);

    java.util.List<Examination> findByOrganizationIdAndBranchIdAndAcademicYear(String organizationId, String branchId, String academicYear);

    boolean existsByOrganizationIdAndBranchIdAndAcademicYearAndName(String organizationId, String branchId, String academicYear, String name);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndBranchId(String organizationId, String branchId);
}
