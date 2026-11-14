package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@CompoundIndex(name = "org_idx", def = "{'organizationId': 1}")
@Document(collection = "exam_subjects")
@CompoundIndexes({
    @CompoundIndex(name = "exam_subject_unique", def = "{'examinationId': 1, 'subjectName': 1, 'classId': 1}", unique = true),
    @CompoundIndex(name = "exam_class", def = "{'examinationId': 1, 'classId': 1}")
})
public class ExamSubject {

    @Id
    private String id;

    private String examinationId;

    private String organizationId;

    private String branchId;

    private String classId;

    private String subjectName;

    /** Catalog id of the subject (null for records created before the catalog). */
    private String subjectId;

    private String subjectCode;

    private Double maxMarks;

    private Double passingMarks;

    private LocalDate examDate;

    private ExamSubjectStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ExamSubjectStatus {
        SCHEDULED, COMPLETED, CANCELLED
    }
}
