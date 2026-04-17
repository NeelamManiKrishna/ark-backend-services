package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.ExamSubject.ExamSubjectStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateExamSubjectRequest {

    private String subjectName;

    private String subjectCode;

    private Double maxMarks;

    private Double passingMarks;

    private LocalDate examDate;

    private ExamSubjectStatus status;
}
