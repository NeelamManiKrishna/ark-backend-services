package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateExamSubjectRequest {

    @NotBlank
    private String classId;

    private String subjectName;

    /** Alternative to subjectName: id from the subject catalog. */
    private String subjectId;

    private String subjectCode;

    @NotNull
    @Positive
    private Double maxMarks;

    @NotNull
    @Positive
    private Double passingMarks;

    private LocalDate examDate;
}
