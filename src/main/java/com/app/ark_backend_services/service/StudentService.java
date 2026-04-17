package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.CreateStudentRequest;
import com.app.ark_backend_services.dto.StudentResponse;
import com.app.ark_backend_services.dto.UpdateStudentRequest;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.Student;
import com.app.ark_backend_services.model.Student.StudentStatus;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.StudentEnrollment;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.ExamResultRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.StudentEnrollmentRepository;
import com.app.ark_backend_services.repository.StudentRepository;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExamResultRepository examResultRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public StudentResponse create(String organizationId, CreateStudentRequest request) {
        validateOrgAccess(organizationId);
        validateOrganizationExists(organizationId);
        validateBranchExists(organizationId, request.getBranchId());

        Student student = new Student();
        student.setArkId(ArkIdGenerator.generateStudentId());
        student.setOrganizationId(organizationId);
        student.setBranchId(request.getBranchId());
        student.setRollNumber(request.getRollNumber());
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress());
        student.setCity(request.getCity());
        student.setState(request.getState());
        student.setZipCode(request.getZipCode());
        student.setGuardianName(request.getGuardianName());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianEmail(request.getGuardianEmail());
        student.setGovtIdType(request.getGovtIdType());
        student.setGovtIdNumber(request.getGovtIdNumber());
        student.setEnrollmentDate(request.getEnrollmentDate());
        student.setStatus(StudentStatus.ACTIVE);

        Student savedStudent = studentRepository.save(student);

        // Auto-create initial enrollment if classId and academicYear are provided
        if (request.getClassId() != null && request.getAcademicYear() != null) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setArkId(ArkIdGenerator.generateEnrollmentId());
            enrollment.setOrganizationId(organizationId);
            enrollment.setBranchId(request.getBranchId());
            enrollment.setStudentId(savedStudent.getId());
            enrollment.setClassId(request.getClassId());
            enrollment.setAcademicYear(request.getAcademicYear());
            enrollment.setEnrolledAt(request.getEnrollmentDate() != null ? request.getEnrollmentDate() : java.time.LocalDate.now());
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(enrollment);
        }

        StudentResponse response = StudentResponse.from(savedStudent);
        auditLogService.log(Action.CREATE, "Student", response.getId(), student.getFirstName() + " " + student.getLastName(), organizationId, "Student created: " + student.getRollNumber());
        return response;
    }

    public StudentResponse getById(String organizationId, String studentId) {
        validateOrgAccess(organizationId);
        Student student = studentRepository.findByIdAndOrganizationId(studentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return StudentResponse.from(student);
    }

    public Page<StudentResponse> getAllByOrganization(String organizationId, Pageable pageable) {
        validateOrgAccess(organizationId);
        validateOrganizationExists(organizationId);
        return studentRepository.findByOrganizationId(organizationId, pageable).map(StudentResponse::from);
    }

    public Page<StudentResponse> getAllByBranch(String organizationId, String branchId, Pageable pageable) {
        validateOrgAccess(organizationId);
        return studentRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getAllByClass(String organizationId, String classId, Pageable pageable) {
        validateOrgAccess(organizationId);

        // Query via active enrollments — Student.classId is deprecated
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findByOrganizationIdAndClassIdAndStatus(organizationId, classId, EnrollmentStatus.ACTIVE);
        List<String> studentIds = enrollments.stream()
                .map(StudentEnrollment::getStudentId)
                .collect(Collectors.toList());

        if (studentIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Student> students = studentRepository.findByIdInAndOrganizationId(studentIds, organizationId);

        // Manual pagination over the result set
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), students.size());
        List<StudentResponse> page = (start >= students.size())
                ? List.of()
                : students.subList(start, end).stream().map(StudentResponse::from).toList();

        return new PageImpl<>(page, pageable, students.size());
    }

    public StudentResponse update(String organizationId, String studentId, UpdateStudentRequest request) {
        validateOrgAccess(organizationId);
        Student student = studentRepository.findByIdAndOrganizationId(studentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (request.getRollNumber() != null) {
            student.setRollNumber(request.getRollNumber());
        }
        if (request.getBranchId() != null) {
            student.setBranchId(request.getBranchId());
        }
        // classId is managed via StudentEnrollment — not updatable directly
        if (request.getFirstName() != null) {
            student.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            student.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            student.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            student.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            student.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }
        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            student.setCity(request.getCity());
        }
        if (request.getState() != null) {
            student.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            student.setZipCode(request.getZipCode());
        }
        if (request.getGuardianName() != null) {
            student.setGuardianName(request.getGuardianName());
        }
        if (request.getGuardianPhone() != null) {
            student.setGuardianPhone(request.getGuardianPhone());
        }
        if (request.getGuardianEmail() != null) {
            student.setGuardianEmail(request.getGuardianEmail());
        }
        if (request.getGovtIdType() != null) {
            student.setGovtIdType(request.getGovtIdType());
        }
        if (request.getGovtIdNumber() != null) {
            student.setGovtIdNumber(request.getGovtIdNumber());
        }
        if (request.getEnrollmentDate() != null) {
            student.setEnrollmentDate(request.getEnrollmentDate());
        }
        if (request.getStatus() != null) {
            student.setStatus(request.getStatus());
        }

        StudentResponse response = StudentResponse.from(studentRepository.save(student));
        auditLogService.log(Action.UPDATE, "Student", studentId, student.getFirstName() + " " + student.getLastName(), organizationId, "Student updated");
        return response;
    }

    @Transactional
    public void delete(String organizationId, String studentId) {
        validateOrgAccess(organizationId);
        Student student = studentRepository.findByIdAndOrganizationId(studentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        examResultRepository.deleteByStudentId(studentId);
        enrollmentRepository.deleteByStudentId(studentId);
        auditLogService.log(Action.DELETE, "Student", studentId, student.getFirstName() + " " + student.getLastName(), organizationId, "Student deleted with enrollments and results: " + student.getRollNumber());
        studentRepository.delete(student);
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }

    private void validateOrganizationExists(String organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }
    }

    private void validateBranchExists(String organizationId, String branchId) {
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
    }
}
