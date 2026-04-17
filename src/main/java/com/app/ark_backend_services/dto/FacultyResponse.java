package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Faculty;
import com.app.ark_backend_services.model.Faculty.FacultyStatus;
import com.app.ark_backend_services.model.Faculty.GovtIdType;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
public class FacultyResponse {

    private String id;
    private String arkId;
    private String organizationId;
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
    private Instant createdAt;
    private Instant updatedAt;

    public static FacultyResponse from(Faculty faculty) {
        FacultyResponse response = new FacultyResponse();
        response.setId(faculty.getId());
        response.setArkId(faculty.getArkId());
        response.setOrganizationId(faculty.getOrganizationId());
        response.setBranchId(faculty.getBranchId());
        response.setEmployeeId(faculty.getEmployeeId());
        response.setFirstName(faculty.getFirstName());
        response.setLastName(faculty.getLastName());
        response.setEmail(faculty.getEmail());
        response.setPhone(faculty.getPhone());
        response.setDateOfBirth(faculty.getDateOfBirth());
        response.setGender(faculty.getGender());
        response.setAddress(faculty.getAddress());
        response.setCity(faculty.getCity());
        response.setState(faculty.getState());
        response.setZipCode(faculty.getZipCode());
        response.setDepartment(faculty.getDepartment());
        response.setDesignation(faculty.getDesignation());
        response.setQualifications(faculty.getQualifications());
        response.setSpecializations(faculty.getSpecializations());
        response.setJoiningDate(faculty.getJoiningDate());
        response.setGovtIdType(faculty.getGovtIdType());
        response.setGovtIdNumber(faculty.getGovtIdNumber());
        response.setStatus(faculty.getStatus());
        response.setCreatedAt(faculty.getCreatedAt());
        response.setUpdatedAt(faculty.getUpdatedAt());
        return response;
    }
}