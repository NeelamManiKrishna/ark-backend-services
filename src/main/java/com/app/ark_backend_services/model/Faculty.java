package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Document(collection = "faculty")
public class Faculty {

    @Id
    private String id;

    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
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

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum FacultyStatus {
        ACTIVE, INACTIVE, ON_LEAVE, RESIGNED, TERMINATED
    }

    public enum GovtIdType {
        AADHAAR, PAN, PASSPORT, DRIVING_LICENSE, VOTER_ID, OTHER
    }
}