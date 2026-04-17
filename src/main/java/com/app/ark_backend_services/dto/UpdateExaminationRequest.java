package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Examination.ExamStatus;
import com.app.ark_backend_services.model.Examination.ExamType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateExaminationRequest {

    private String name;

    private String academicYear;

    private ExamType examType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

    private ExamStatus status;
}
