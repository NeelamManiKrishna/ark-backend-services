package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateEnrollmentRequest {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Class ID is required")
    private String classId;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    private LocalDate enrolledAt;
}
