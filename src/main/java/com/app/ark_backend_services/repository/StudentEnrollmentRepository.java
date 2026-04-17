package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.StudentEnrollment;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudentEnrollmentRepository extends MongoRepository<StudentEnrollment, String> {

    Optional<StudentEnrollment> findByStudentIdAndStatus(String studentId, EnrollmentStatus status);

    Optional<StudentEnrollment> findByStudentIdAndAcademicYear(String studentId, String academicYear);

    List<StudentEnrollment> findByStudentIdOrderByAcademicYearDesc(String studentId);

    List<StudentEnrollment> findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(String organizationId, String studentId);

    Page<StudentEnrollment> findByClassIdAndStatus(String classId, EnrollmentStatus status, Pageable pageable);

    Page<StudentEnrollment> findByOrganizationIdAndClassIdAndStatus(String organizationId, String classId, EnrollmentStatus status, Pageable pageable);

    List<StudentEnrollment> findByOrganizationIdAndClassIdAndStatus(String organizationId, String classId, EnrollmentStatus status);

    Page<StudentEnrollment> findByOrganizationIdAndBranchIdAndAcademicYear(String organizationId, String branchId, String academicYear, Pageable pageable);

    boolean existsByStudentIdAndStatus(String studentId, EnrollmentStatus status);

    boolean existsByStudentIdAndAcademicYear(String studentId, String academicYear);

    long countByClassIdAndStatus(String classId, EnrollmentStatus status);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndBranchId(String organizationId, String branchId);

    void deleteByStudentId(String studentId);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}
