package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.Faculty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FacultyRepository extends MongoRepository<Faculty, String> {

    Page<Faculty> findByOrganizationId(String organizationId, Pageable pageable);

    Page<Faculty> findByOrganizationIdAndBranchId(String organizationId, String branchId, Pageable pageable);

    Page<Faculty> findByOrganizationIdAndDepartment(String organizationId, String department, Pageable pageable);

    Optional<Faculty> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByOrganizationIdAndEmployeeId(String organizationId, String employeeId);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, Faculty.FacultyStatus status);

    long countByOrganizationIdAndBranchId(String organizationId, String branchId);

    long countByOrganizationIdAndBranchIdAndStatus(String organizationId, String branchId, Faculty.FacultyStatus status);

    long countByOrganizationIdAndBranchIdAndDepartment(String organizationId, String branchId, String department);

    java.util.List<Faculty> findByOrganizationIdAndBranchId(String organizationId, String branchId);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}