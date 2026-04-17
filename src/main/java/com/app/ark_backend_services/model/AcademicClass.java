package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "academic_classes")
@CompoundIndex(name = "org_branch_class_name_section_year", def = "{'organizationId': 1, 'branchId': 1, 'name': 1, 'section': 1, 'academicYear': 1}", unique = true)
public class AcademicClass {

    @Id
    private String id;

    private String organizationId;

    private String branchId;

    private String name;

    private String section;

    private String academicYear;

    private Integer capacity;

    private String description;

    private ClassStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ClassStatus {
        ACTIVE, INACTIVE, COMPLETED
    }
}