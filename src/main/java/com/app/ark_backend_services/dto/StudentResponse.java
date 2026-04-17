package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Student;
import com.app.ark_backend_services.model.Student.GovtIdType;
import com.app.ark_backend_services.model.Student.StudentStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class StudentResponse {

    private String id;
    private String arkId;
    private String organizationId;
    private String branchId;
    private String classId;
    private String rollNumber;
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
    private String guardianName;
    private String guardianPhone;
    private String guardianEmail;
    private GovtIdType govtIdType;
    private String govtIdNumber;
    private LocalDate enrollmentDate;
    private StudentStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static StudentResponse from(Student student) {
        StudentResponse response = new StudentResponse();
        response.setId(student.getId());
        response.setArkId(student.getArkId());
        response.setOrganizationId(student.getOrganizationId());
        response.setBranchId(student.getBranchId());
        response.setClassId(student.getClassId());
        response.setRollNumber(student.getRollNumber());
        response.setFirstName(student.getFirstName());
        response.setLastName(student.getLastName());
        response.setEmail(student.getEmail());
        response.setPhone(student.getPhone());
        response.setDateOfBirth(student.getDateOfBirth());
        response.setGender(student.getGender());
        response.setAddress(student.getAddress());
        response.setCity(student.getCity());
        response.setState(student.getState());
        response.setZipCode(student.getZipCode());
        response.setGuardianName(student.getGuardianName());
        response.setGuardianPhone(student.getGuardianPhone());
        response.setGuardianEmail(student.getGuardianEmail());
        response.setGovtIdType(student.getGovtIdType());
        response.setGovtIdNumber(student.getGovtIdNumber());
        response.setEnrollmentDate(student.getEnrollmentDate());
        response.setStatus(student.getStatus());
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());
        return response;
    }
}