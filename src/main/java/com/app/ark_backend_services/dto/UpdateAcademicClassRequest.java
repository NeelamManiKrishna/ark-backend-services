package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.AcademicClass.ClassStatus;
import lombok.Data;

@Data
public class UpdateAcademicClassRequest {

    private String name;
    private String section;
    private String academicYear;
    private Integer capacity;
    private String description;
    private ClassStatus status;
}
