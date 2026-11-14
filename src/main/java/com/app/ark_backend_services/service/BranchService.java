package com.app.ark_backend_services.service;

import com.app.ark_backend_services.util.AuditSnapshot;
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
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    private final SearchService searchService;

    public BranchResponse create(String organizationId, CreateBranchRequest request) {
        AccessPolicy.assertOrg(organizationId);
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
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        return BranchResponse.from(branch);
    }

    public Page<BranchResponse> getAllByOrganization(String organizationId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        validateOrganizationExists(organizationId);
        String ownBranch = AccessPolicy.restrictBranch(null);
        if (ownBranch != null) {
            // Branch-scoped roles only see their own branch
            var own = branchRepository.findByIdAndOrganizationId(ownBranch, organizationId)
                    .map(b -> java.util.List.of(BranchResponse.from(b))).orElse(java.util.List.of());
            return new org.springframework.data.domain.PageImpl<>(own, pageable, own.size());
        }
        return branchRepository.findByOrganizationId(organizationId, pageable).map(BranchResponse::from);
    }

    public BranchResponse update(String organizationId, String branchId, UpdateBranchRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        var auditBefore = AuditSnapshot.of(branch);

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
        auditLogService.logChange(Action.UPDATE, "Branch", branchId, branch.getName(), organizationId, "Branch updated", auditBefore, AuditSnapshot.of(branch));
        return response;
    }

    public void delete(String organizationId, String branchId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        String cascade = auditLogService.countCascade(org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("organizationId").is(organizationId).and("branchId").is(branchId)),
                "users", "student_enrollments", "faculty_assignments", "class_progressions", "exam_results", "exam_subjects", "examinations", "faculty", "students", "academic_classes");
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
        branchRepository.delete(branch);
        auditLogService.log(Action.DELETE, "Branch", branchId, branch.getName(), organizationId, "Branch deleted with all related data: " + cascade);
    }


    private void validateOrganizationExists(String organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }
    }

    /** Prefix search ("q") over branch name, city and ARK id; branch-scoped callers only ever see their own branch. */
    public Page<BranchResponse> search(String organizationId, String q, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        String ownBranch = AccessPolicy.restrictBranch(null);
        org.springframework.data.mongodb.core.query.Criteria scope = org.springframework.data.mongodb.core.query.Criteria.where("organizationId").is(organizationId);
        if (ownBranch != null) {
            scope = scope.and("_id").is(ownBranch);
        }
        return searchService.search(Branch.class, scope, q,
                java.util.List.of("name", "city", "arkId"), pageable, BranchResponse::from);
    }
}
