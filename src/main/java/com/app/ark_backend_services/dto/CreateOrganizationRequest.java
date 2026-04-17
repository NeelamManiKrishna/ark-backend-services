package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    private String address;

    @Email(message = "Contact email must be a valid email address")
    private String contactEmail;

    private String contactPhone;
    private String website;
    private String logoUrl;
}
