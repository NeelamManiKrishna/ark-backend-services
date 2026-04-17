package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAcademicClassRequest {

    @NotBlank(message = "Class name is required")
    private String name;

    private String section;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    private Integer capacity;
    private String description;
}
