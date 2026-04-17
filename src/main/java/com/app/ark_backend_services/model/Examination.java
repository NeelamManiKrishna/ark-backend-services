package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "examinations")
@CompoundIndexes({
    @CompoundIndex(name = "org_branch_year_name", def = "{'organizationId': 1, 'branchId': 1, 'academicYear': 1, 'name': 1}", unique = true),
    @CompoundIndex(name = "org_branch_year", def = "{'organizationId': 1, 'branchId': 1, 'academicYear': 1}")
})
public class Examination {

    @Id
    private String id;

    @Indexed(unique = true)
    private String arkId;

    private String organizationId;

    private String branchId;

    private String name;

    private String academicYear;

    private ExamType examType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

    private ExamStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ExamType {
        MIDTERM, FINAL, QUARTERLY, HALF_YEARLY, UNIT_TEST, SUPPLEMENTARY
    }

    public enum ExamStatus {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
