package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.AcademicClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AcademicClassRepository extends MongoRepository<AcademicClass, String> {

    Page<AcademicClass> findByOrganizationIdAndBranchId(String organizationId, String branchId, Pageable pageable);

    Optional<AcademicClass> findByIdAndOrganizationIdAndBranchId(String id, String organizationId, String branchId);

    boolean existsByOrganizationIdAndBranchIdAndNameAndSection(String organizationId, String branchId, String name, String section);

    boolean existsByOrganizationIdAndBranchIdAndNameAndSectionAndAcademicYear(String organizationId, String branchId, String name, String section, String academicYear);

    java.util.Optional<AcademicClass> findByOrganizationIdAndBranchIdAndNameAndSectionAndAcademicYear(String organizationId, String branchId, String name, String section, String academicYear);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndBranchId(String organizationId, String branchId);

    java.util.List<AcademicClass> findByOrganizationIdAndBranchId(String organizationId, String branchId);

    void deleteByOrganizationId(String organizationId);
}