package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {

    Page<Student> findByOrganizationId(String organizationId, Pageable pageable);

    Page<Student> findByOrganizationIdAndBranchId(String organizationId, String branchId, Pageable pageable);

    Page<Student> findByOrganizationIdAndClassId(String organizationId, String classId, Pageable pageable);

    Optional<Student> findByIdAndOrganizationId(String id, String organizationId);

    List<Student> findByIdInAndOrganizationId(Collection<String> ids, String organizationId);

    boolean existsByOrganizationIdAndRollNumber(String organizationId, String rollNumber);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, Student.StudentStatus status);

    long countByOrganizationIdAndBranchId(String organizationId, String branchId);

    long countByOrganizationIdAndGender(String organizationId, String gender);

    long countByOrganizationIdAndBranchIdAndStatus(String organizationId, String branchId, Student.StudentStatus status);

    long countByOrganizationIdAndBranchIdAndGender(String organizationId, String branchId, String gender);

    long countByOrganizationIdAndClassId(String organizationId, String classId);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}