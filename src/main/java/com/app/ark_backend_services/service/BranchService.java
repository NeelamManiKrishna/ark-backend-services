package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.BranchResponse;
import com.app.ark_backend_services.dto.CreateBranchRequest;
import com.app.ark_backend_services.dto.UpdateBranchRequest;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.Branch;
import com.app.ark_backend_services.model.Branch.BranchStatus;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.ClassProgressionRepository;
import com.app.ark_backend_services.repository.ExamResultRepository;
import com.app.ark_backend_services.repository.ExamSubjectRepository;
import com.app.ark_backend_services.repository.ExaminationRepository;
import com.app.ark_backend_services.repository.FacultyAssignmentRepository;
import com.app.ark_backend_services.repository.FacultyRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.StudentEnrollmentRepository;
import com.app.ark_backend_services.repository.StudentRepository;
import com.app.ark_backend_services.repository.UserRepository;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final OrganizationRepository organizationRepository;
    private final AcademicClassRepository academicClassRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamResultRepository examResultRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final ClassProgressionRepository progressionRepository;
    private final AuditLogService auditLogService;

    public BranchResponse create(String organizationId, CreateBranchRequest request) {
        validateOrgAccess(organizationId);
        validateOrganizationExists(organizationId);

        if (branchRepository.existsByOrganizationIdAndName(organizationId, request.getName())) {
            throw new DuplicateResourceException("Branch with name '" + request.getName() + "' already exists in this organization");
        }

        Branch branch = new Branch();
        branch.setArkId(ArkIdGenerator.generateBranchId());
        branch.setOrganizationId(organizationId);
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setState(request.getState());
        branch.setZipCode(request.getZipCode());
        branch.setContactEmail(request.getContactEmail());
        branch.setContactPhone(request.getContactPhone());
        branch.setStatus(BranchStatus.ACTIVE);

        BranchResponse response = BranchResponse.from(branchRepository.save(branch));
        auditLogService.log(Action.CREATE, "Branch", response.getId(), branch.getName(), organizationId, "Branch created");
        return response;
    }

    public BranchResponse getById(String organizationId, String branchId) {
        validateOrgAccess(organizationId);
        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        return BranchResponse.from(branch);
    }

    public Page<BranchResponse> getAllByOrganization(String organizationId, Pageable pageable) {
        validateOrgAccess(organizationId);
        validateOrganizationExists(organizationId);
        return branchRepository.findByOrganizationId(organizationId, pageable).map(BranchResponse::from);
    }

    public BranchResponse update(String organizationId, String branchId, UpdateBranchRequest request) {
        validateOrgAccess(organizationId);
        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        if (request.getName() != null && !request.getName().equals(branch.getName())) {
            if (branchRepository.existsByOrganizationIdAndName(organizationId, request.getName())) {
                throw new DuplicateResourceException("Branch with name '" + request.getName() + "' already exists in this organization");
            }
            branch.setName(request.getName());
        }
        if (request.getAddress() != null) {
            branch.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            branch.setCity(request.getCity());
        }
        if (request.getState() != null) {
            branch.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            branch.setZipCode(request.getZipCode());
        }
        if (request.getContactEmail() != null) {
            branch.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            branch.setContactPhone(request.getContactPhone());
        }
        if (request.getStatus() != null) {
            branch.setStatus(request.getStatus());
        }

        BranchResponse response = BranchResponse.from(branchRepository.save(branch));
        auditLogService.log(Action.UPDATE, "Branch", branchId, branch.getName(), organizationId, "Branch updated");
        return response;
    }

    @Transactional
    public void delete(String organizationId, String branchId) {
        validateOrgAccess(organizationId);
        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        userRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        examResultRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        examSubjectRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        enrollmentRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        assignmentRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        progressionRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        examinationRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        facultyRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        studentRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        academicClassRepository.deleteByOrganizationIdAndBranchId(organizationId, branchId);
        auditLogService.log(Action.DELETE, "Branch", branchId, branch.getName(), organizationId, "Branch deleted with all related data");
        branchRepository.delete(branch);
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
}
