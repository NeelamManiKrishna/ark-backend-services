package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Faculty.FacultyStatus;
import com.app.ark_backend_services.model.Faculty.GovtIdType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateFacultyRequest {

    private String branchId;
    private String employeeId;
    private String firstName;
    private String lastName;
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
    private GovtIdType govtIdType;
    private String govtIdNumber;
    private FacultyStatus status;
}