package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.AcademicClass;
import com.app.ark_backend_services.model.AcademicClass.ClassStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class AcademicClassResponse {

    private String id;
    private String organizationId;
    private String branchId;
    private String name;
    private String section;
    private String academicYear;
    private Integer capacity;
    private String description;
    private ClassStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static AcademicClassResponse from(AcademicClass ac) {
        AcademicClassResponse response = new AcademicClassResponse();
        response.setId(ac.getId());
        response.setOrganizationId(ac.getOrganizationId());
        response.setBranchId(ac.getBranchId());
        response.setName(ac.getName());
        response.setSection(ac.getSection());
        response.setAcademicYear(ac.getAcademicYear());
        response.setCapacity(ac.getCapacity());
        response.setDescription(ac.getDescription());
        response.setStatus(ac.getStatus());
        response.setCreatedAt(ac.getCreatedAt());
        response.setUpdatedAt(ac.getUpdatedAt());
        return response;
    }
}
