package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Organization.OrganizationStatus;
import lombok.Data;

@Data
public class UpdateOrganizationRequest {

    private String name;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private String logoUrl;
    private OrganizationStatus status;
}
