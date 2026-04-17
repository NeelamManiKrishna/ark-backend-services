package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "student_enrollments")
@CompoundIndexes({
        @CompoundIndex(name = "student_year_unique", def = "{'studentId': 1, 'academicYear': 1}", unique = true),
        @CompoundIndex(name = "org_class_status", def = "{'organizationId': 1, 'classId': 1, 'status': 1}"),
        @CompoundIndex(name = "student_status", def = "{'studentId': 1, 'status': 1}"),
        @CompoundIndex(name = "org_branch_year", def = "{'organizationId': 1, 'branchId': 1, 'academicYear': 1}")
})
public class StudentEnrollment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String arkId;

    private String organizationId;

    private String branchId;

    private String studentId;

    private String classId;

    private String academicYear;

    private LocalDate enrolledAt;

    private LocalDate exitedAt;

    private ExitReason exitReason;

    private EnrollmentStatus status;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum EnrollmentStatus {
        ACTIVE, COMPLETED
    }

    public enum ExitReason {
        PROMOTED, GRADUATED, HELD_BACK, TRANSFERRED, DROPPED
    }
}
