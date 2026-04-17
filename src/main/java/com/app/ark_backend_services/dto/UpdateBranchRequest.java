package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Branch.BranchStatus;
import lombok.Data;

@Data
public class UpdateBranchRequest {

    private String name;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String contactEmail;
    private String contactPhone;
    private BranchStatus status;
}
