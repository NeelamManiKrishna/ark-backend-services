package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.AcademicClassResponse;
import com.app.ark_backend_services.dto.CreateAcademicClassRequest;
import com.app.ark_backend_services.dto.UpdateAcademicClassRequest;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AcademicClass;
import com.app.ark_backend_services.model.AcademicClass.ClassStatus;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.StudentEnrollmentRepository;
import com.app.ark_backend_services.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcademicClassService {

    private final AcademicClassRepository academicClassRepository;
    private final BranchRepository branchRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    public AcademicClassResponse create(String organizationId, String branchId, CreateAcademicClassRequest request) {
        validateOrgAccess(organizationId);
        validateBranchExists(organizationId, branchId);

        if (academicClassRepository.existsByOrganizationIdAndBranchIdAndNameAndSectionAndAcademicYear(organizationId, branchId, request.getName(), request.getSection(), request.getAcademicYear())) {
            throw new DuplicateResourceException("Class '" + request.getName() + " " + request.getSection() + "' already exists for academic year " + request.getAcademicYear() + " in this branch");
        }

        AcademicClass ac = new AcademicClass();
        ac.setOrganizationId(organizationId);
        ac.setBranchId(branchId);
        ac.setName(request.getName());
        ac.setSection(request.getSection());
        ac.setAcademicYear(request.getAcademicYear());
        ac.setCapacity(request.getCapacity());
        ac.setDescription(request.getDescription());
        ac.setStatus(ClassStatus.ACTIVE);

        AcademicClassResponse response = AcademicClassResponse.from(academicClassRepository.save(ac));
        auditLogService.log(Action.CREATE, "AcademicClass", response.getId(), ac.getName(), organizationId, "Class created: " + ac.getName() + " " + ac.getSection());
        return response;
    }

    public AcademicClassResponse getById(String organizationId, String branchId, String classId) {
        validateOrgAccess(organizationId);
        AcademicClass ac = academicClassRepository.findByIdAndOrganizationIdAndBranchId(classId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        return AcademicClassResponse.from(ac);
    }

    public Page<AcademicClassResponse> getAllByBranch(String organizationId, String branchId, Pageable pageable) {
        validateOrgAccess(organizationId);
        validateBranchExists(organizationId, branchId);
        return academicClassRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                .map(AcademicClassResponse::from);
    }

    public AcademicClassResponse update(String organizationId, String branchId, String classId, UpdateAcademicClassRequest request) {
        validateOrgAccess(organizationId);
        AcademicClass ac = academicClassRepository.findByIdAndOrganizationIdAndBranchId(classId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (request.getName() != null && !request.getName().equals(ac.getName())) {
            String section = request.getSection() != null ? request.getSection() : ac.getSection();
            String year = request.getAcademicYear() != null ? request.getAcademicYear() : ac.getAcademicYear();
            if (academicClassRepository.existsByOrganizationIdAndBranchIdAndNameAndSectionAndAcademicYear(organizationId, branchId, request.getName(), section, year)) {
                throw new DuplicateResourceException("Class '" + request.getName() + " " + section + "' already exists for academic year " + year + " in this branch");
            }
            ac.setName(request.getName());
        }
        if (request.getSection() != null) {
            ac.setSection(request.getSection());
        }
        if (request.getAcademicYear() != null) {
            ac.setAcademicYear(request.getAcademicYear());
        }
        if (request.getCapacity() != null) {
            ac.setCapacity(request.getCapacity());
        }
        if (request.getDescription() != null) {
            ac.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            ac.setStatus(request.getStatus());
        }

        AcademicClassResponse response = AcademicClassResponse.from(academicClassRepository.save(ac));
        auditLogService.log(Action.UPDATE, "AcademicClass", classId, ac.getName(), organizationId, "Class updated");
        return response;
    }

    public void delete(String organizationId, String branchId, String classId) {
        validateOrgAccess(organizationId);
        AcademicClass ac = academicClassRepository.findByIdAndOrganizationIdAndBranchId(classId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        long activeEnrollments = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        if (activeEnrollments > 0) {
            throw new IllegalStateException("Cannot delete class with " + activeEnrollments
                    + " active enrollment(s). Close or transfer enrollments first.");
        }

        auditLogService.log(Action.DELETE, "AcademicClass", classId, ac.getName(), organizationId, "Class deleted");
        academicClassRepository.delete(ac);
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }

    private void validateBranchExists(String organizationId, String branchId) {
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
    }
}
