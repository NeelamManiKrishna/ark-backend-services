package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBranchRequest {

    @NotBlank(message = "Branch name is required")
    private String name;

    private String address;
    private String city;
    private String state;
    private String zipCode;

    @Email(message = "Contact email must be a valid email address")
    private String contactEmail;

    private String contactPhone;
}
