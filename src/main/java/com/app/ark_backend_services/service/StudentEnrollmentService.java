package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.StudentEnrollment;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.StudentEnrollmentRepository;
import com.app.ark_backend_services.repository.StudentRepository;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentEnrollmentService {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AcademicClassRepository academicClassRepository;
    private final AuditLogService auditLogService;

    public StudentEnrollmentResponse enroll(String organizationId, String branchId, CreateEnrollmentRequest request) {
        validateOrgAccess(organizationId);

        studentRepository.findByIdAndOrganizationId(request.getStudentId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.getStudentId()));

        academicClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

        if (enrollmentRepository.existsByStudentIdAndStatus(request.getStudentId(), EnrollmentStatus.ACTIVE)) {
            throw new DuplicateResourceException("Student already has an active enrollment. Close the current enrollment before creating a new one.");
        }

        if (enrollmentRepository.existsByStudentIdAndAcademicYear(request.getStudentId(), request.getAcademicYear())) {
            throw new DuplicateResourceException("Student already has an enrollment for academic year " + request.getAcademicYear());
        }

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setArkId(ArkIdGenerator.generateEnrollmentId());
        enrollment.setOrganizationId(organizationId);
        enrollment.setBranchId(branchId);
        enrollment.setStudentId(request.getStudentId());
        enrollment.setClassId(request.getClassId());
        enrollment.setAcademicYear(request.getAcademicYear());
        enrollment.setEnrolledAt(request.getEnrolledAt() != null ? request.getEnrolledAt() : LocalDate.now());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        StudentEnrollmentResponse response = StudentEnrollmentResponse.from(enrollmentRepository.save(enrollment));
        auditLogService.log(Action.CREATE, "StudentEnrollment", response.getId(),
                "Enrollment for student " + request.getStudentId(), organizationId,
                "Enrolled in class " + request.getClassId() + " for " + request.getAcademicYear());
        return response;
    }

    public StudentEnrollmentResponse getActiveEnrollment(String organizationId, String studentId) {
        validateOrgAccess(organizationId);
        StudentEnrollment enrollment = enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active enrollment found for student: " + studentId));
        return StudentEnrollmentResponse.from(enrollment);
    }

    public List<StudentEnrollmentResponse> getEnrollmentHistory(String organizationId, String studentId) {
        validateOrgAccess(organizationId);
        return enrollmentRepository.findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(organizationId, studentId)
                .stream()
                .map(StudentEnrollmentResponse::from)
                .toList();
    }

    public Page<StudentEnrollmentResponse> getEnrollmentsByClass(String organizationId, String classId, Pageable pageable) {
        validateOrgAccess(organizationId);
        return enrollmentRepository.findByOrganizationIdAndClassIdAndStatus(organizationId, classId, EnrollmentStatus.ACTIVE, pageable)
                .map(StudentEnrollmentResponse::from);
    }

    public Page<StudentEnrollmentResponse> getEnrollmentsByBranchAndYear(String organizationId, String branchId, String academicYear, Pageable pageable) {
        validateOrgAccess(organizationId);
        return enrollmentRepository.findByOrganizationIdAndBranchIdAndAcademicYear(organizationId, branchId, academicYear, pageable)
                .map(StudentEnrollmentResponse::from);
    }

    public StudentEnrollmentResponse closeEnrollment(String organizationId, String enrollmentId, CloseEnrollmentRequest request) {
        validateOrgAccess(organizationId);
        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(e -> e.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new IllegalStateException("Enrollment is already closed");
        }

        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setExitReason(request.getExitReason());
        enrollment.setExitedAt(request.getExitedAt() != null ? request.getExitedAt() : LocalDate.now());

        StudentEnrollmentResponse response = StudentEnrollmentResponse.from(enrollmentRepository.save(enrollment));
        auditLogService.log(Action.UPDATE, "StudentEnrollment", enrollmentId,
                "Enrollment closed for student " + enrollment.getStudentId(), organizationId,
                "Exit reason: " + request.getExitReason());
        return response;
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }
}
