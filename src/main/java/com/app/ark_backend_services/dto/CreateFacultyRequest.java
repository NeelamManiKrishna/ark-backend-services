package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Faculty.GovtIdType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateFacultyRequest {

    @NotBlank(message = "Branch ID is required")
    private String branchId;

    private String employeeId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email must be a valid email address")
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String department;
    private String designation;
    private List<String> qualifications;
    private List<String> specializations;
    private LocalDate joiningDate;
    @NotNull(message = "Government ID type is required")
    private GovtIdType govtIdType;

    @NotBlank(message = "Government ID number is required")
    private String govtIdNumber;
}
