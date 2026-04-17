package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "students")
public class Student {

    @Id
    private String id;

    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String arkId;

    private String organizationId;

    private String branchId;

    @Deprecated // Use StudentEnrollment as source of truth; kept for backward compatibility (dual-write)
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

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum StudentStatus {
        ACTIVE, INACTIVE, GRADUATED, TRANSFERRED, DROPPED
    }

    public enum GovtIdType {
        AADHAAR, PAN, PASSPORT, DRIVING_LICENSE, VOTER_ID, OTHER
    }
}