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
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyAssignmentService {

    private final FacultyAssignmentRepository assignmentRepository;
    private final FacultyRepository facultyRepository;
    private final AcademicClassRepository academicClassRepository;
    private final AuditLogService auditLogService;

    public FacultyAssignmentResponse create(String organizationId, String branchId, CreateFacultyAssignmentRequest request) {
        validateOrgAccess(organizationId);

        facultyRepository.findByIdAndOrganizationId(request.getFacultyId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found: " + request.getFacultyId()));

        academicClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

        String subjectName = request.getSubjectName() != null ? request.getSubjectName() : "";
        if (assignmentRepository.existsByFacultyIdAndClassIdAndSubjectNameAndAcademicYear(
                request.getFacultyId(), request.getClassId(), subjectName, request.getAcademicYear())) {
            throw new DuplicateResourceException("Faculty already assigned to this class/subject for academic year " + request.getAcademicYear());
        }

        FacultyAssignment assignment = new FacultyAssignment();
        assignment.setArkId(ArkIdGenerator.generateAssignmentId());
        assignment.setOrganizationId(organizationId);
        assignment.setBranchId(branchId);
        assignment.setFacultyId(request.getFacultyId());
        assignment.setClassId(request.getClassId());
        assignment.setSubjectName(subjectName);
        assignment.setAcademicYear(request.getAcademicYear());
        assignment.setAssignmentType(request.getAssignmentType());
        assignment.setStatus(AssignmentStatus.ACTIVE);

        FacultyAssignmentResponse response = FacultyAssignmentResponse.from(assignmentRepository.save(assignment));
        auditLogService.log(Action.CREATE, "FacultyAssignment", response.getId(),
                "Assignment for faculty " + request.getFacultyId(), organizationId,
                "Assigned to class " + request.getClassId() + " as " + request.getAssignmentType());
        return response;
    }

    public FacultyAssignmentResponse getById(String organizationId, String assignmentId) {
        validateOrgAccess(organizationId);
        FacultyAssignment assignment = assignmentRepository.findByIdAndOrganizationId(assignmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        return FacultyAssignmentResponse.from(assignment);
    }

    public List<FacultyAssignmentResponse> getActiveByFaculty(String organizationId, String facultyId) {
        validateOrgAccess(organizationId);
        return assignmentRepository.findByFacultyIdAndStatus(facultyId, AssignmentStatus.ACTIVE)
                .stream()
                .map(FacultyAssignmentResponse::from)
                .toList();
    }

    public Page<FacultyAssignmentResponse> getAllByFaculty(String organizationId, String facultyId, Pageable pageable) {
        validateOrgAccess(organizationId);
        return assignmentRepository.findByFacultyId(facultyId, pageable)
                .map(FacultyAssignmentResponse::from);
    }

    public Page<FacultyAssignmentResponse> getByClassAndYear(String organizationId, String classId, String academicYear, Pageable pageable) {
        validateOrgAccess(organizationId);
        return assignmentRepository.findByOrganizationIdAndClassIdAndAcademicYear(organizationId, classId, academicYear, pageable)
                .map(FacultyAssignmentResponse::from);
    }

    public Page<FacultyAssignmentResponse> getByBranchAndYear(String organizationId, String branchId, String academicYear, Pageable pageable) {
        validateOrgAccess(organizationId);
        return assignmentRepository.findByOrganizationIdAndBranchIdAndAcademicYear(organizationId, branchId, academicYear, pageable)
                .map(FacultyAssignmentResponse::from);
    }

    public FacultyAssignmentResponse update(String organizationId, String assignmentId, UpdateFacultyAssignmentRequest request) {
        validateOrgAccess(organizationId);
        FacultyAssignment assignment = assignmentRepository.findByIdAndOrganizationId(assignmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

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

    public void delete(String organizationId, String assignmentId) {
        validateOrgAccess(organizationId);
        FacultyAssignment assignment = assignmentRepository.findByIdAndOrganizationId(assignmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        auditLogService.log(Action.DELETE, "FacultyAssignment", assignmentId,
                "Assignment for faculty " + assignment.getFacultyId(), organizationId, "Assignment deleted");
        assignmentRepository.delete(assignment);
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }
}
