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

@Data
@Document(collection = "faculty_assignments")
@CompoundIndexes({
        @CompoundIndex(name = "faculty_class_subject_year_unique", def = "{'facultyId': 1, 'classId': 1, 'subjectName': 1, 'academicYear': 1}", unique = true),
        @CompoundIndex(name = "org_class_year", def = "{'organizationId': 1, 'classId': 1, 'academicYear': 1}"),
        @CompoundIndex(name = "faculty_status", def = "{'facultyId': 1, 'status': 1}"),
        @CompoundIndex(name = "org_branch_year", def = "{'organizationId': 1, 'branchId': 1, 'academicYear': 1}")
})
public class FacultyAssignment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String arkId;

    private String organizationId;

    private String branchId;

    private String facultyId;

    private String classId;

    private String subjectName;

    private String academicYear;

    private AssignmentType assignmentType;

    private AssignmentStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum AssignmentType {
        SUBJECT_TEACHER, CLASS_TEACHER, BOTH
    }

    public enum AssignmentStatus {
        ACTIVE, COMPLETED
    }
}
