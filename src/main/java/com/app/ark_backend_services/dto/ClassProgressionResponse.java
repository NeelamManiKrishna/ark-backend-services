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
