package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAcademicClassRequest {

    @NotBlank(message = "Class name is required")
    private String name;

    private String section;

    private String academicYear;

    /** Alternative to academicYear: id from the academic year catalog. */
    private String academicYearId;

    private Integer capacity;
    private String description;
}
