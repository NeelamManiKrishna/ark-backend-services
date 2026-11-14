package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Examination.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateExaminationRequest {

    @NotBlank(message = "Exam name is required")
    private String name;

    private String academicYear;

    /** Alternative to academicYear: id from the academic year catalog. */
    private String academicYearId;

    @NotNull(message = "Exam type is required")
    private ExamType examType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;
}
