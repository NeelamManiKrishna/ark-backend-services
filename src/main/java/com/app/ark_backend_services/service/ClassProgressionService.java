package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.ClassProgressionRequest;
import com.app.ark_backend_services.dto.ClassProgressionResponse;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.ClassProgression;
import com.app.ark_backend_services.model.ClassProgression.ClassLevel;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.ClassProgressionRepository;
import com.app.ark_backend_services.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassProgressionService {

    private final ClassProgressionRepository progressionRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public ClassProgressionResponse upsert(String organizationId, String branchId, ClassProgressionRequest request) {
        validateOrgAccess(organizationId);
        validateBranchExists(organizationId, branchId);

        ClassProgression progression = progressionRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                .orElseGet(() -> {
                    ClassProgression p = new ClassProgression();
                    p.setOrganizationId(organizationId);
                    p.setBranchId(branchId);
                    return p;
                });

        List<ClassLevel> sequence = request.getSequence().stream().map(dto -> {
            ClassLevel level = new ClassLevel();
            level.setClassName(dto.getClassName());
            level.setDisplayOrder(dto.getDisplayOrder());
            level.setIsTerminal(dto.getIsTerminal() != null ? dto.getIsTerminal() : false);
            return level;
        }).toList();

        progression.setSequence(sequence);

        ClassProgressionResponse response = ClassProgressionResponse.from(progressionRepository.save(progression));
        auditLogService.log(Action.UPDATE, "ClassProgression", response.getId(),
                "Class progression for branch " + branchId, organizationId,
                "Sequence updated with " + sequence.size() + " levels");
        return response;
    }

    public ClassProgressionResponse get(String organizationId, String branchId) {
        validateOrgAccess(organizationId);
        ClassProgression progression = progressionRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class progression not configured for branch: " + branchId));
        return ClassProgressionResponse.from(progression);
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }

    private void validateBranchExists(String organizationId, String branchId) {
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }
}
