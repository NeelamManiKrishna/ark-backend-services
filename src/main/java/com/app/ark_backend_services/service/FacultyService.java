package com.app.ark_backend_services.service;

import com.app.ark_backend_services.util.AuditSnapshot;
import com.app.ark_backend_services.dto.CreateFacultyRequest;
import com.app.ark_backend_services.dto.FacultyResponse;
import com.app.ark_backend_services.dto.UpdateFacultyRequest;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.Faculty;
import com.app.ark_backend_services.model.Faculty.FacultyStatus;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.FacultyAssignmentRepository;
import com.app.ark_backend_services.repository.FacultyRepository;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;
    private final SearchService searchService;

    public FacultyResponse create(String organizationId, CreateFacultyRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(request.getBranchId());
        validateOrganizationExists(organizationId);
        validateBranchExists(organizationId, request.getBranchId());

        Faculty faculty = new Faculty();
        faculty.setArkId(ArkIdGenerator.generateFacultyId());
        faculty.setOrganizationId(organizationId);
        faculty.setBranchId(request.getBranchId());
        faculty.setEmployeeId(request.getEmployeeId());
        faculty.setFirstName(request.getFirstName());
        faculty.setLastName(request.getLastName());
        faculty.setEmail(request.getEmail());
        faculty.setPhone(request.getPhone());
        faculty.setDateOfBirth(request.getDateOfBirth());
        faculty.setGender(com.app.ark_backend_services.util.Genders.normalize(request.getGender()));
        faculty.setAddress(request.getAddress());
        faculty.setCity(request.getCity());
        faculty.setState(request.getState());
        faculty.setZipCode(request.getZipCode());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());
        faculty.setQualifications(request.getQualifications());
        faculty.setSpecializations(request.getSpecializations());
        faculty.setJoiningDate(request.getJoiningDate());
        faculty.setGovtIdType(request.getGovtIdType());
        faculty.setGovtIdNumber(request.getGovtIdNumber());
        faculty.setStatus(FacultyStatus.ACTIVE);

        FacultyResponse response = FacultyResponse.from(facultyRepository.save(faculty));
        auditLogService.log(Action.CREATE, "Faculty", response.getId(), faculty.getFirstName() + " " + faculty.getLastName(), organizationId, "Faculty created: " + faculty.getEmployeeId());
        return response;
    }

    public FacultyResponse getById(String organizationId, String facultyId) {
        AccessPolicy.assertOrg(organizationId);
        Faculty faculty = facultyRepository.findByIdAndOrganizationId(facultyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
        AccessPolicy.assertBranch(faculty.getBranchId());
        return FacultyResponse.from(faculty);
    }

    public Page<FacultyResponse> getAllByOrganization(String organizationId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        validateOrganizationExists(organizationId);
        String branchId = AccessPolicy.restrictBranch(null);
        if (branchId != null) {
            return facultyRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                    .map(FacultyResponse::from);
        }
        return facultyRepository.findByOrganizationId(organizationId, pageable).map(FacultyResponse::from);
    }

    public Page<FacultyResponse> getAllByBranch(String organizationId, String branchId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        return facultyRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                .map(FacultyResponse::from);
    }

    public Page<FacultyResponse> getAllByDepartment(String organizationId, String department, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        String branchId = AccessPolicy.restrictBranch(null);
        if (branchId != null) {
            return facultyRepository.findByOrganizationIdAndBranchIdAndDepartment(organizationId, branchId, department, pageable)
                    .map(FacultyResponse::from);
        }
        return facultyRepository.findByOrganizationIdAndDepartment(organizationId, department, pageable)
                .map(FacultyResponse::from);
    }

    public FacultyResponse update(String organizationId, String facultyId, UpdateFacultyRequest request) {
        AccessPolicy.assertOrg(organizationId);
        Faculty faculty = facultyRepository.findByIdAndOrganizationId(facultyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
        var auditBefore = AuditSnapshot.of(faculty);
        AccessPolicy.assertBranch(faculty.getBranchId());

        if (request.getEmployeeId() != null) {
            faculty.setEmployeeId(request.getEmployeeId());
        }
        if (request.getBranchId() != null && !request.getBranchId().equals(faculty.getBranchId())) {
            AccessPolicy.assertBranch(request.getBranchId());
            validateBranchExists(organizationId, request.getBranchId());
            if (assignmentRepository.existsByOrganizationIdAndFacultyIdAndStatus(organizationId, facultyId,
                    com.app.ark_backend_services.model.FacultyAssignment.AssignmentStatus.ACTIVE)) {
                throw new IllegalStateException("Cannot move faculty with active assignments to another branch. End the assignments first.");
            }
            faculty.setBranchId(request.getBranchId());
        }
        if (request.getFirstName() != null) {
            faculty.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            faculty.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            faculty.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            faculty.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            faculty.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            faculty.setGender(com.app.ark_backend_services.util.Genders.normalize(request.getGender()));
        }
        if (request.getAddress() != null) {
            faculty.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            faculty.setCity(request.getCity());
        }
        if (request.getState() != null) {
            faculty.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            faculty.setZipCode(request.getZipCode());
        }
        if (request.getDepartment() != null) {
            faculty.setDepartment(request.getDepartment());
        }
        if (request.getDesignation() != null) {
            faculty.setDesignation(request.getDesignation());
        }
        if (request.getQualifications() != null) {
            faculty.setQualifications(request.getQualifications());
        }
        if (request.getSpecializations() != null) {
            faculty.setSpecializations(request.getSpecializations());
        }
        if (request.getJoiningDate() != null) {
            faculty.setJoiningDate(request.getJoiningDate());
        }
        if (request.getGovtIdType() != null) {
            faculty.setGovtIdType(request.getGovtIdType());
        }
        if (request.getGovtIdNumber() != null) {
            faculty.setGovtIdNumber(request.getGovtIdNumber());
        }
        if (request.getStatus() != null) {
            faculty.setStatus(request.getStatus());
        }

        FacultyResponse response = FacultyResponse.from(facultyRepository.save(faculty));
        auditLogService.logChange(Action.UPDATE, "Faculty", facultyId, faculty.getFirstName() + " " + faculty.getLastName(), organizationId, "Faculty updated", auditBefore, AuditSnapshot.of(faculty));
        return response;
    }

    public void delete(String organizationId, String facultyId) {
        AccessPolicy.assertOrg(organizationId);
        Faculty faculty = facultyRepository.findByIdAndOrganizationId(facultyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
        AccessPolicy.assertBranch(faculty.getBranchId());
        assignmentRepository.deleteByFacultyId(facultyId);
        facultyRepository.delete(faculty);
        auditLogService.log(Action.DELETE, "Faculty", facultyId, faculty.getFirstName() + " " + faculty.getLastName(), organizationId, "Faculty deleted with assignments: " + faculty.getEmployeeId());
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

    /** Prefix search ("q") over name, email, ARK id and employee id, within the caller's scope. */
    public Page<FacultyResponse> search(String organizationId, String branchId, String q, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        String effectiveBranch = AccessPolicy.restrictBranch(branchId);
        org.springframework.data.mongodb.core.query.Criteria scope = org.springframework.data.mongodb.core.query.Criteria.where("organizationId").is(organizationId);
        if (effectiveBranch != null) {
            scope = scope.and("branchId").is(effectiveBranch);
        }
        return searchService.search(Faculty.class, scope, q,
                java.util.List.of("firstName", "lastName", "email", "arkId", "employeeId"), pageable, FacultyResponse::from);
    }
}
