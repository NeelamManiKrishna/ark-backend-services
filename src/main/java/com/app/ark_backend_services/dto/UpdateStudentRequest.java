package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Student.GovtIdType;
import com.app.ark_backend_services.model.Student.StudentStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStudentRequest {

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
}