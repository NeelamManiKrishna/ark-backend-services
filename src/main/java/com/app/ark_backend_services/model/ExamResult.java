package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "exam_results")
@CompoundIndexes({
    @CompoundIndex(name = "result_unique", def = "{'examSubjectId': 1, 'studentId': 1}", unique = true),
    @CompoundIndex(name = "exam_student", def = "{'examinationId': 1, 'studentId': 1}"),
    @CompoundIndex(name = "org_exam", def = "{'organizationId': 1, 'examinationId': 1}")
})
public class ExamResult {

    @Id
    private String id;

    private String examinationId;

    private String examSubjectId;

    private String organizationId;

    private String branchId;

    private String classId;

    private String studentId;

    private Double marksObtained;

    private String grade;

    private String remarks;

    private ResultStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ResultStatus {
        PASS, FAIL, ABSENT, WITHHELD
    }
}
