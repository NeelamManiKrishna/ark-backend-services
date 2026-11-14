package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.ClassProgression;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ClassProgressionResponse {

    private String id;
    private String organizationId;
    private String branchId;
    private List<ClassProgression.ClassLevel> sequence;
    /** Levels that have no class in this branch yet (informational: promotion creates the next class on demand). */
    private List<String> classesNotCreatedYet = List.of();
    private Instant createdAt;
    private Instant updatedAt;

    public static ClassProgressionResponse from(ClassProgression progression) {
        ClassProgressionResponse response = new ClassProgressionResponse();
        response.setId(progression.getId());
        response.setOrganizationId(progression.getOrganizationId());
        response.setBranchId(progression.getBranchId());
        response.setSequence(progression.getSequence());
        response.setCreatedAt(progression.getCreatedAt());
        response.setUpdatedAt(progression.getUpdatedAt());
        return response;
    }
}
