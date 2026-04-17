package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Organization;
import com.app.ark_backend_services.model.Organization.OrganizationStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class OrganizationResponse {

    private String id;
    private String arkId;
    private String name;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private String logoUrl;
    private OrganizationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static OrganizationResponse from(Organization org) {
        OrganizationResponse response = new OrganizationResponse();
        response.setId(org.getId());
        response.setArkId(org.getArkId());
        response.setName(org.getName());
        response.setAddress(org.getAddress());
        response.setContactEmail(org.getContactEmail());
        response.setContactPhone(org.getContactPhone());
        response.setWebsite(org.getWebsite());
        response.setLogoUrl(org.getLogoUrl());
        response.setStatus(org.getStatus());
        response.setCreatedAt(org.getCreatedAt());
        response.setUpdatedAt(org.getUpdatedAt());
        return response;
    }
}
