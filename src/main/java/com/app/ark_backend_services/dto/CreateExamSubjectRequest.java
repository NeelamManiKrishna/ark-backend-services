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

    @NotBlank
    private String subjectName;

    private String subjectCode;

    @NotNull
    @Positive
    private Double maxMarks;

    @NotNull
    @Positive
    private Double passingMarks;

    private LocalDate examDate;
}
