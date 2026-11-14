package com.app.ark_backend_services.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import com.app.ark_backend_services.security.EncryptedStringConverter;
import lombok.Data;
import org.springframework.data.convert.ValueConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@CompoundIndex(name = "org_branch_idx", def = "{'organizationId': 1, 'branchId': 1}")
@CompoundIndex(name = "org_status_idx", def = "{'organizationId': 1, 'status': 1}")
@Document(collection = "students")
public class Student {

    @Id
    private String id;

    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String arkId;

    private String organizationId;

    private String branchId;

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

    @ValueConverter(EncryptedStringConverter.class)
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
}