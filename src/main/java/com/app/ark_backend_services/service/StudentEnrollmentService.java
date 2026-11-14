package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AcademicClass;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.Student;
import com.app.ark_backend_services.model.StudentEnrollment;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.StudentEnrollmentRepository;
import com.app.ark_backend_services.repository.StudentRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final CatalogResolver catalogResolver;

    public StudentEnrollmentResponse enroll(String organizationId, String branchId, CreateEnrollmentRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);

        var year = catalogResolver.year(organizationId, request.getAcademicYearId(), request.getAcademicYear());
        request.setAcademicYear(year.label());
        loadStudentInBranch(organizationId, branchId, request.getStudentId());

        // The class must exist in this organization AND this branch
        academicClassRepository.findByIdAndOrganizationIdAndBranchId(request.getClassId(), organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

        if (enrollmentRepository.existsByOrganizationIdAndStudentIdAndStatus(organizationId, request.getStudentId(), EnrollmentStatus.ACTIVE)) {
            throw new DuplicateResourceException("Student already has an active enrollment. Close the current enrollment before creating a new one.");
        }

        if (enrollmentRepository.existsByOrganizationIdAndStudentIdAndAcademicYear(organizationId, request.getStudentId(), request.getAcademicYear())) {
            throw new DuplicateResourceException("Student already has an enrollment for academic year " + request.getAcademicYear());
        }

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setArkId(ArkIdGenerator.generateEnrollmentId());
        enrollment.setOrganizationId(organizationId);
        enrollment.setBranchId(branchId);
        enrollment.setStudentId(request.getStudentId());
        enrollment.setClassId(request.getClassId());
        enrollment.setAcademicYear(request.getAcademicYear());
        enrollment.setAcademicYearId(year.id());
        enrollment.setEnrolledAt(request.getEnrolledAt() != null ? request.getEnrolledAt() : LocalDate.now());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        StudentEnrollmentResponse response = StudentEnrollmentResponse.from(enrollmentRepository.save(enrollment));
        auditLogService.log(Action.CREATE, "StudentEnrollment", response.getId(),
                "Enrollment for student " + request.getStudentId(), organizationId,
                "Enrolled in class " + request.getClassId() + " for " + request.getAcademicYear());
        return response;
    }

    public StudentEnrollmentResponse getActiveEnrollment(String organizationId, String branchId, String studentId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        loadStudentInBranch(organizationId, branchId, studentId);
        StudentEnrollment enrollment = enrollmentRepository
                .findByOrganizationIdAndStudentIdAndStatus(organizationId, studentId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active enrollment found for student: " + studentId));
        return StudentEnrollmentResponse.from(enrollment);
    }

    public List<StudentEnrollmentResponse> getEnrollmentHistory(String organizationId, String branchId, String studentId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        loadStudentInBranch(organizationId, branchId, studentId);
        return enrollmentRepository.findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(organizationId, studentId, org.springframework.data.domain.PageRequest.of(0, 200))
                .getContent().stream()
                .map(StudentEnrollmentResponse::from)
                .toList();
    }

    public Page<StudentEnrollmentResponse> getEnrollmentsByClass(String organizationId, String branchId, String classId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        AcademicClass ac = academicClassRepository.findByIdAndOrganizationIdAndBranchId(classId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        return enrollmentRepository.findByOrganizationIdAndClassIdAndStatus(organizationId, ac.getId(), EnrollmentStatus.ACTIVE, pageable)
                .map(StudentEnrollmentResponse::from);
    }

    public Page<StudentEnrollmentResponse> getEnrollmentsByBranchAndYear(String organizationId, String branchId, String academicYear, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        return enrollmentRepository.findByOrganizationIdAndBranchIdAndAcademicYear(organizationId, branchId, academicYear, pageable)
                .map(StudentEnrollmentResponse::from);
    }

    public StudentEnrollmentResponse closeEnrollment(String organizationId, String branchId, String enrollmentId, CloseEnrollmentRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        StudentEnrollment enrollment = enrollmentRepository.findByIdAndOrganizationId(enrollmentId, organizationId)
                .filter(e -> branchId.equals(e.getBranchId()))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new IllegalStateException("Enrollment is already closed");
        }

        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setExitReason(request.getExitReason());
        enrollment.setExitedAt(request.getExitedAt() != null ? request.getExitedAt() : LocalDate.now());

        StudentEnrollmentResponse response = StudentEnrollmentResponse.from(enrollmentRepository.save(enrollment));

        // Leaving the school for good changes the student's own status too
        Student.StudentStatus newStatus = switch (request.getExitReason()) {
            case GRADUATED -> Student.StudentStatus.GRADUATED;
            case TRANSFERRED -> Student.StudentStatus.TRANSFERRED;
            case DROPPED -> Student.StudentStatus.DROPPED;
            default -> null; // PROMOTED / HELD_BACK: student stays ACTIVE
        };
        if (newStatus != null) {
            studentRepository.findByIdAndOrganizationId(enrollment.getStudentId(), organizationId).ifPresent(st -> {
                st.setStatus(newStatus);
                studentRepository.save(st);
            });
        }

        auditLogService.log(Action.UPDATE, "StudentEnrollment", enrollmentId,
                "Enrollment closed for student " + enrollment.getStudentId(), organizationId,
                "Exit reason: " + request.getExitReason());
        return response;
    }

    /** Undo a closure, only for the student's most recent enrollment and when they have no active one. */
    public StudentEnrollmentResponse reopenEnrollment(String organizationId, String branchId, String enrollmentId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        StudentEnrollment enrollment = enrollmentRepository.findByIdAndOrganizationId(enrollmentId, organizationId)
                .filter(e -> branchId.equals(e.getBranchId()))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new IllegalStateException("Only closed enrollments can be reopened");
        }
        if (enrollmentRepository.existsByOrganizationIdAndStudentIdAndAcademicYearGreaterThan(
                organizationId, enrollment.getStudentId(), enrollment.getAcademicYear())
                || enrollmentRepository.existsByOrganizationIdAndStudentIdAndStatus(
                organizationId, enrollment.getStudentId(), EnrollmentStatus.ACTIVE)) {
            throw new IllegalStateException("Only the student's latest enrollment can be reopened, and only if they have no active enrollment");
        }

        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setExitReason(null);
        enrollment.setExitedAt(null);
        StudentEnrollmentResponse response = StudentEnrollmentResponse.from(enrollmentRepository.save(enrollment));

        studentRepository.findByIdAndOrganizationId(enrollment.getStudentId(), organizationId).ifPresent(st -> {
            if (st.getStatus() != Student.StudentStatus.ACTIVE) {
                st.setStatus(Student.StudentStatus.ACTIVE);
                studentRepository.save(st);
            }
        });

        auditLogService.log(Action.UPDATE, "StudentEnrollment", enrollmentId,
                "Enrollment reopened for student " + enrollment.getStudentId(), organizationId, "Enrollment reopened");
        return response;
    }

    /** Student must exist in the organization and belong to the branch in the path. */
    private Student loadStudentInBranch(String organizationId, String branchId, String studentId) {
        Student student = studentRepository.findByIdAndOrganizationId(studentId, organizationId)
                .filter(s -> branchId.equals(s.getBranchId()))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        return student;
    }
}
