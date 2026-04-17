package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.CreateOrganizationRequest;
import com.app.ark_backend_services.dto.OrganizationResponse;
import com.app.ark_backend_services.dto.UpdateOrganizationRequest;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.Organization;
import com.app.ark_backend_services.model.Organization.OrganizationStatus;
import com.app.ark_backend_services.repository.*;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final AcademicClassRepository academicClassRepository;
    private final StudentRepository studentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamResultRepository examResultRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final ClassProgressionRepository progressionRepository;
    private final AuditLogService auditLogService;

    public OrganizationResponse create(CreateOrganizationRequest request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Organization with name '" + request.getName() + "' already exists");
        }

        Organization org = new Organization();
        org.setArkId(ArkIdGenerator.generateOrgId());
        org.setName(request.getName());
        org.setAddress(request.getAddress());
        org.setContactEmail(request.getContactEmail());
        org.setContactPhone(request.getContactPhone());
        org.setWebsite(request.getWebsite());
        org.setLogoUrl(request.getLogoUrl());
        org.setStatus(OrganizationStatus.ACTIVE);

        OrganizationResponse response = OrganizationResponse.from(organizationRepository.save(org));
        auditLogService.log(Action.CREATE, "Organization", response.getId(), org.getName(), response.getId(), "Organization created");
        return response;
    }

    public OrganizationResponse getById(String id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return OrganizationResponse.from(org);
    }

    public Page<OrganizationResponse> getAll(Pageable pageable) {
        return organizationRepository.findAll(pageable).map(OrganizationResponse::from);
    }

    public OrganizationResponse update(String id, UpdateOrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(org.getName())) {
            if (organizationRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Organization with name '" + request.getName() + "' already exists");
            }
            org.setName(request.getName());
        }
        if (request.getAddress() != null) {
            org.setAddress(request.getAddress());
        }
        if (request.getContactEmail() != null) {
            org.setContactEmail(request.getContactEmail());
        }

        if (request.getContactPhone() != null) {
            org.setContactPhone(request.getContactPhone());
        }
        if (request.getWebsite() != null) {
            org.setWebsite(request.getWebsite());
        }
        if (request.getLogoUrl() != null) {
            org.setLogoUrl(request.getLogoUrl());
        }
        if (request.getStatus() != null) {
            org.setStatus(request.getStatus());
        }

        OrganizationResponse response = OrganizationResponse.from(organizationRepository.save(org));
        auditLogService.log(Action.UPDATE, "Organization", id, org.getName(), id, "Organization updated");
        return response;
    }

    @Transactional
    public void delete(String id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found with id: " + id);
        }
        userRepository.deleteByOrganizationId(id);
        enrollmentRepository.deleteByOrganizationId(id);
        assignmentRepository.deleteByOrganizationId(id);
        progressionRepository.deleteByOrganizationId(id);
        examResultRepository.deleteByOrganizationId(id);
        examSubjectRepository.deleteByOrganizationId(id);
        examinationRepository.deleteByOrganizationId(id);
        facultyRepository.deleteByOrganizationId(id);
        studentRepository.deleteByOrganizationId(id);
        academicClassRepository.deleteByOrganizationId(id);
        branchRepository.deleteByOrganizationId(id);
        auditLogService.log(Action.DELETE, "Organization", id, null, id, "Organization deleted with all related data");
        organizationRepository.deleteById(id);
    }
}