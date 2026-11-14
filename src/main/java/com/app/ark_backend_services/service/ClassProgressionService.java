package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.ClassProgressionRequest;
import com.app.ark_backend_services.dto.ClassProgressionResponse;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.ClassProgression;
import com.app.ark_backend_services.model.ClassProgression.ClassLevel;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.ClassProgressionRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassProgressionService {

    private final ClassProgressionRepository progressionRepository;
    private final AcademicClassRepository academicClassRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public ClassProgressionResponse upsert(String organizationId, String branchId, ClassProgressionRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        validateBranchExists(organizationId, branchId);

        validateSequence(request);

        boolean isNew = progressionRepository.findByOrganizationIdAndBranchId(organizationId, branchId).isEmpty();
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

        ClassProgressionResponse response = withMissingClasses(ClassProgressionResponse.from(progressionRepository.save(progression)), organizationId, branchId);
        auditLogService.log(isNew ? Action.CREATE : Action.UPDATE, "ClassProgression", response.getId(),
                "Class progression for branch " + branchId, organizationId,
                "Sequence updated with " + sequence.size() + " levels");
        return response;
    }

    private ClassProgressionResponse withMissingClasses(ClassProgressionResponse response, String organizationId, String branchId) {
        response.setClassesNotCreatedYet(response.getSequence().stream()
                .map(ClassLevel::getClassName)
                .filter(name -> !academicClassRepository.existsByOrganizationIdAndBranchIdAndName(organizationId, branchId, name))
                .toList());
        return response;
    }

    /**
     * The sequence must be gap-free (orders 1..n, promotion looks for "order + 1"), names must be unique, and
     * exactly one level must be terminal and it must be the last one.
     */
    private void validateSequence(ClassProgressionRequest request) {
        java.util.Set<Integer> orders = new java.util.HashSet<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        int terminals = 0;
        for (var dto : request.getSequence()) {
            if (!orders.add(dto.getDisplayOrder())) {
                throw new IllegalArgumentException("Duplicate displayOrder " + dto.getDisplayOrder() + " in sequence");
            }
            if (!names.add(dto.getClassName())) {
                throw new IllegalArgumentException("Duplicate class name '" + dto.getClassName() + "' in sequence");
            }
            if (Boolean.TRUE.equals(dto.getIsTerminal())) {
                terminals++;
            }
        }
        if (terminals != 1) {
            throw new IllegalArgumentException("Exactly one level must be marked terminal (found " + terminals + ")");
        }
        int n = request.getSequence().size();
        for (int expected = 1; expected <= n; expected++) {
            if (!orders.contains(expected)) {
                throw new IllegalArgumentException("displayOrder must run 1.." + n + " without gaps (missing " + expected + ")");
            }
        }
        request.getSequence().stream().filter(l -> Boolean.TRUE.equals(l.getIsTerminal())).findFirst().ifPresent(t -> {
            if (t.getDisplayOrder() != n) {
                throw new IllegalArgumentException("The terminal level must be the last one (displayOrder " + n + ")");
            }
        });
    }

    public void delete(String organizationId, String branchId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        ClassProgression progression = progressionRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class progression not configured for branch: " + branchId));
        progressionRepository.delete(progression);
        auditLogService.log(Action.DELETE, "ClassProgression", progression.getId(),
                "Class progression for branch " + branchId, organizationId, "Class progression deleted");
    }

    public ClassProgressionResponse get(String organizationId, String branchId) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        ClassProgression progression = progressionRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class progression not configured for branch: " + branchId));
        return withMissingClasses(ClassProgressionResponse.from(progression), organizationId, branchId);
    }


    private void validateBranchExists(String organizationId, String branchId) {
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }
}
