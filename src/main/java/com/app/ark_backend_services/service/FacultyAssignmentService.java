package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.FacultyAssignment;
import com.app.ark_backend_services.model.FacultyAssignment.AssignmentStatus;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.FacultyAssignmentRepository;
import com.app.ark_backend_services.repository.FacultyRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyAssignmentService {

    private final FacultyAssignmentRepository assignmentRepository;
    private final FacultyRepository facultyRepository;
    private final AcademicClassRepository academicClassRepository;
    private final AuditLogService auditLogService;
    private final CatalogResolver catalogResolver;

    public FacultyAssignmentResponse create(String organizationId, String branchId, CreateFacultyAssignmentRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);

        var year = catalogResolver.year(organizationId, request.getAcademicYearId(), request.getAcademicYear());
        request.setAcademicYear(year.label());
        var subject = catalogResolver.subject(organizationId, request.getSubjectId(), request.getSubjectName(), false);
        request.setSubjectName(subject.name());
        loadFacultyInBranch(organizationId, branchId, request.getFacultyId());

        // The class must exist in this organization AND this branch
        academicClassRepository.findByIdAndOrganizationIdAndBranchId(request.getClassId(), organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

        String subjectName = request.getSubjectName() != null ? request.getSubjectName() : "";
        if (assignmentRepository.existsByOrganizationIdAndFacultyIdAndClassIdAndSubjectNameAndAcademicYear(
                organizationId, request.getFacultyId(), request.getClassId(), subjectName, request.getAcademicYear())) {
            throw new DuplicateResourceException("Faculty already assigned to this class/subject for academic year " + request.getAcademicYear());
        }

        FacultyAssignment assignment = new FacultyAssignment();
        assignment.setArkId(ArkIdGenerator.generateAssignmentId());
        assignment.setOrganizationId(organizationId);
        assignment.setBranchId(branchId);
        assignment.setFacultyId(request.getFacultyId());
        assignment.setClassId(request.getClassId());
        assignment.setSubjectName(subjectName);
        assignment.setSubjectId(subject.id());
        assignment.setAcademicYear(request.getAcademicYear());
        assignment.setAcademicYearId(year.id());
        assignment.setAssignmentType(request.getAssignmentType());
        assignment.setStatus(AssignmentStatus.ACTIVE);

        FacultyAssignmentResponse response = FacultyAssignmentResponse.from(assignmentRepository.save(assignment));
        auditLogService.log(Action.CREATE, "FacultyAssignment", response.getId(),
                "Assignment for faculty " + request.getFacultyId(), organizationId,
                "Assigned to class " + request.getClassId() + " as " + request.getAssignmentType());
        return response;
    }

    public FacultyAssignmentResponse getById(String organizationId, String branchId, String assignmentId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        return FacultyAssignmentResponse.from(loadAssignment(organizationId, branchId, assignmentId));
    }

    public List<FacultyAssignmentResponse> getActiveByFaculty(String organizationId, String branchId, String facultyId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        loadFacultyInBranch(organizationId, branchId, facultyId);
        return assignmentRepository.findByOrganizationIdAndFacultyIdAndStatus(organizationId, facultyId, AssignmentStatus.ACTIVE, org.springframework.data.domain.PageRequest.of(0, 200))
                .getContent().stream()
                .map(FacultyAssignmentResponse::from)
                .toList();
    }

    public Page<FacultyAssignmentResponse> getAllByFaculty(String organizationId, String branchId, String facultyId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        loadFacultyInBranch(organizationId, branchId, facultyId);
        return assignmentRepository.findByOrganizationIdAndFacultyId(organizationId, facultyId, pageable)
                .map(FacultyAssignmentResponse::from);
    }

    public Page<FacultyAssignmentResponse> getByClassAndYear(String organizationId, String branchId, String classId, String academicYear, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        academicClassRepository.findByIdAndOrganizationIdAndBranchId(classId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        return assignmentRepository.findByOrganizationIdAndClassIdAndAcademicYear(organizationId, classId, academicYear, pageable)
                .map(FacultyAssignmentResponse::from);
    }

    public Page<FacultyAssignmentResponse> getByBranchAndYear(String organizationId, String branchId, String academicYear, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        return assignmentRepository.findByOrganizationIdAndBranchIdAndAcademicYear(organizationId, branchId, academicYear, pageable)
                .map(FacultyAssignmentResponse::from);
    }

    public FacultyAssignmentResponse update(String organizationId, String branchId, String assignmentId, UpdateFacultyAssignmentRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        FacultyAssignment assignment = loadAssignment(organizationId, branchId, assignmentId);

        if (request.getAssignmentType() != null) {
            assignment.setAssignmentType(request.getAssignmentType());
        }
        if (request.getStatus() != null) {
            assignment.setStatus(request.getStatus());
        }

        FacultyAssignmentResponse response = FacultyAssignmentResponse.from(assignmentRepository.save(assignment));
        auditLogService.log(Action.UPDATE, "FacultyAssignment", assignmentId,
                "Assignment for faculty " + assignment.getFacultyId(), organizationId, "Assignment updated");
        return response;
    }

    public void delete(String organizationId, String branchId, String assignmentId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        FacultyAssignment assignment = loadAssignment(organizationId, branchId, assignmentId);
        assignmentRepository.delete(assignment);
        auditLogService.log(Action.DELETE, "FacultyAssignment", assignmentId,
                "Assignment for faculty " + assignment.getFacultyId(), organizationId, "Assignment deleted");
    }

    private FacultyAssignment loadAssignment(String organizationId, String branchId, String assignmentId) {
        return assignmentRepository.findByIdAndOrganizationIdAndBranchId(assignmentId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
    }

    private void loadFacultyInBranch(String organizationId, String branchId, String facultyId) {
        facultyRepository.findByIdAndOrganizationId(facultyId, organizationId)
                .filter(f -> branchId.equals(f.getBranchId()))
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found: " + facultyId));
    }
}
