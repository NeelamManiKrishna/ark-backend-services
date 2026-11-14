package com.app.ark_backend_services.service;

import com.app.ark_backend_services.util.AuditSnapshot;
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
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExamResultRepository examResultRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;
    private final CatalogResolver catalogResolver;
    private final SearchService searchService;
    private final com.app.ark_backend_services.repository.AcademicClassRepository academicClassRepository;

    public StudentResponse create(String organizationId, CreateStudentRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(request.getBranchId());
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
        student.setGender(com.app.ark_backend_services.util.Genders.normalize(request.getGender()));
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
        if (request.getClassId() != null && (request.getAcademicYear() != null || request.getAcademicYearId() != null)) {
            var year = catalogResolver.year(organizationId, request.getAcademicYearId(), request.getAcademicYear());
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setArkId(ArkIdGenerator.generateEnrollmentId());
            enrollment.setOrganizationId(organizationId);
            enrollment.setBranchId(request.getBranchId());
            enrollment.setStudentId(savedStudent.getId());
            enrollment.setClassId(request.getClassId());
            enrollment.setAcademicYear(year.label());
            enrollment.setAcademicYearId(year.id());
            enrollment.setEnrolledAt(request.getEnrollmentDate() != null ? request.getEnrollmentDate() : java.time.LocalDate.now());
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(enrollment);
        }

        StudentResponse response = StudentResponse.from(savedStudent);
        auditLogService.log(Action.CREATE, "Student", response.getId(), student.getFirstName() + " " + student.getLastName(), organizationId, "Student created: " + student.getRollNumber());
        return response;
    }

    public StudentResponse getById(String organizationId, String studentId) {
        AccessPolicy.assertOrg(organizationId);
        Student student = studentRepository.findByIdAndOrganizationId(studentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        AccessPolicy.assertBranch(student.getBranchId());
        return withActiveClass(organizationId, List.of(StudentResponse.from(student))).get(0);
    }

    public Page<StudentResponse> getAllByOrganization(String organizationId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        validateOrganizationExists(organizationId);
        String branchId = AccessPolicy.restrictBranch(null);
        if (branchId != null) {
            return withActiveClass(organizationId, studentRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                    .map(StudentResponse::from));
        }
        return withActiveClass(organizationId, studentRepository.findByOrganizationId(organizationId, pageable).map(StudentResponse::from));
    }

    public Page<StudentResponse> getAllByBranch(String organizationId, String branchId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        return withActiveClass(organizationId, studentRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                .map(StudentResponse::from));
    }

    public Page<StudentResponse> getAllByClass(String organizationId, String classId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);

        // Active enrollments give the roster; students are then paged and sorted by the database
        List<String> studentIds = enrollmentRepository
                .findByOrganizationIdAndClassIdAndStatus(organizationId, classId, EnrollmentStatus.ACTIVE).stream()
                .map(StudentEnrollment::getStudentId)
                .collect(Collectors.toList());

        if (studentIds.isEmpty()) {
            return Page.empty(pageable);
        }

        String allowedBranch = AccessPolicy.restrictBranch(null);
        Page<Student> students = allowedBranch != null
                ? studentRepository.findByIdInAndOrganizationIdAndBranchId(studentIds, organizationId, allowedBranch, pageable)
                : studentRepository.findByIdInAndOrganizationId(studentIds, organizationId, pageable);
        return withActiveClass(organizationId, students.map(StudentResponse::from));
    }

    public StudentResponse update(String organizationId, String studentId, UpdateStudentRequest request) {
        AccessPolicy.assertOrg(organizationId);
        Student student = studentRepository.findByIdAndOrganizationId(studentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        var auditBefore = AuditSnapshot.of(student);
        AccessPolicy.assertBranch(student.getBranchId());

        if (request.getRollNumber() != null) {
            student.setRollNumber(request.getRollNumber());
        }
        if (request.getBranchId() != null && !request.getBranchId().equals(student.getBranchId())) {
            AccessPolicy.assertBranch(request.getBranchId());
            validateBranchExists(organizationId, request.getBranchId());
            if (enrollmentRepository.existsByOrganizationIdAndStudentIdAndStatus(organizationId, studentId, EnrollmentStatus.ACTIVE)) {
                throw new IllegalStateException("Cannot move a student with an active enrollment to another branch. Close the enrollment first.");
            }
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
            student.setGender(com.app.ark_backend_services.util.Genders.normalize(request.getGender()));
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
        auditLogService.logChange(Action.UPDATE, "Student", studentId, student.getFirstName() + " " + student.getLastName(), organizationId, "Student updated", auditBefore, AuditSnapshot.of(student));
        return response;
    }

    public void delete(String organizationId, String studentId) {
        AccessPolicy.assertOrg(organizationId);
        Student student = studentRepository.findByIdAndOrganizationId(studentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        AccessPolicy.assertBranch(student.getBranchId());
        examResultRepository.deleteByStudentId(studentId);
        enrollmentRepository.deleteByStudentId(studentId);
        studentRepository.delete(student);
        auditLogService.log(Action.DELETE, "Student", studentId, student.getFirstName() + " " + student.getLastName(), organizationId, "Student deleted with enrollments and results: " + student.getRollNumber());
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

    /** Prefix search ("q") over name, email, ARK id and roll number, within the caller's scope. */
    public Page<StudentResponse> search(String organizationId, String branchId, String classId, String q, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        String effectiveBranch = AccessPolicy.restrictBranch(branchId);
        org.springframework.data.mongodb.core.query.Criteria scope = org.springframework.data.mongodb.core.query.Criteria.where("organizationId").is(organizationId);
        if (effectiveBranch != null) {
            scope = scope.and("branchId").is(effectiveBranch);
        }
        if (classId != null) {
            // Search within the class roster (ACTIVE enrollments); the tenant/branch scope above still applies
            List<String> rosterIds = enrollmentRepository
                    .findByOrganizationIdAndClassIdAndStatus(organizationId, classId, EnrollmentStatus.ACTIVE).stream()
                    .map(StudentEnrollment::getStudentId).collect(Collectors.toList());
            if (rosterIds.isEmpty()) {
                return Page.empty(pageable);
            }
            scope = scope.and("_id").in(rosterIds);
        }
        return withActiveClass(organizationId, searchService.search(Student.class, scope, q,
                java.util.List.of("firstName", "lastName", "email", "arkId", "rollNumber"), pageable, StudentResponse::from));
    }

    private Page<StudentResponse> withActiveClass(String organizationId, Page<StudentResponse> page) {
        withActiveClass(organizationId, page.getContent());
        return page;
    }

    /** Fills activeClassId/activeClassName with one enrollment query and one class query for the whole list. */
    private List<StudentResponse> withActiveClass(String organizationId, List<StudentResponse> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        java.util.Map<String, String> classByStudent = new java.util.HashMap<>();
        for (StudentEnrollment e : enrollmentRepository.findByOrganizationIdAndStudentIdInAndStatus(
                organizationId, rows.stream().map(StudentResponse::getId).toList(), EnrollmentStatus.ACTIVE)) {
            classByStudent.putIfAbsent(e.getStudentId(), e.getClassId());
        }
        java.util.Map<String, String> nameByClass = new java.util.HashMap<>();
        academicClassRepository.findAllById(classByStudent.values().stream().distinct().toList()).forEach(c ->
                nameByClass.put(c.getId(), c.getSection() != null ? c.getName() + " " + c.getSection() : c.getName()));
        for (StudentResponse r : rows) {
            String classId = classByStudent.get(r.getId());
            r.setActiveClassId(classId);
            r.setActiveClassName(classId != null ? nameByClass.get(classId) : null);
        }
        return rows;
    }
}
