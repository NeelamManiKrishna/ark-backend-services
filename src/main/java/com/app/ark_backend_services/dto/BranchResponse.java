package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Branch;
import com.app.ark_backend_services.model.Branch.BranchStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class BranchResponse {

    private String id;
    private String arkId;
    private String organizationId;
    private String name;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String contactEmail;
    private String contactPhone;
    private BranchStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static BranchResponse from(Branch branch) {
        BranchResponse response = new BranchResponse();
        response.setId(branch.getId());
        response.setArkId(branch.getArkId());
        response.setOrganizationId(branch.getOrganizationId());
        response.setName(branch.getName());
        response.setAddress(branch.getAddress());
        response.setCity(branch.getCity());
        response.setState(branch.getState());
        response.setZipCode(branch.getZipCode());
        response.setContactEmail(branch.getContactEmail());
        response.setContactPhone(branch.getContactPhone());
        response.setStatus(branch.getStatus());
        response.setCreatedAt(branch.getCreatedAt());
        response.setUpdatedAt(branch.getUpdatedAt());
        return response;
    }
}
