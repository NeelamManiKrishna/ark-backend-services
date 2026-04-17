package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.FacultyAssignment;
import com.app.ark_backend_services.model.FacultyAssignment.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyAssignmentRepository extends MongoRepository<FacultyAssignment, String> {

    List<FacultyAssignment> findByFacultyIdAndStatus(String facultyId, AssignmentStatus status);

    List<FacultyAssignment> findByFacultyIdOrderByAcademicYearDesc(String facultyId);

    Page<FacultyAssignment> findByFacultyId(String facultyId, Pageable pageable);

    Page<FacultyAssignment> findByOrganizationIdAndClassIdAndAcademicYear(String organizationId, String classId, String academicYear, Pageable pageable);

    List<FacultyAssignment> findByOrganizationIdAndClassIdAndAcademicYear(String organizationId, String classId, String academicYear);

    Page<FacultyAssignment> findByOrganizationIdAndBranchIdAndAcademicYear(String organizationId, String branchId, String academicYear, Pageable pageable);

    Optional<FacultyAssignment> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByFacultyIdAndClassIdAndSubjectNameAndAcademicYear(String facultyId, String classId, String subjectName, String academicYear);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndBranchId(String organizationId, String branchId);

    void deleteByFacultyId(String facultyId);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}
