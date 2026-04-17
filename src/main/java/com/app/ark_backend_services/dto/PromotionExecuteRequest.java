package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PromotionExecuteRequest {

    @NotBlank(message = "Source class ID is required")
    private String sourceClassId;

    @NotBlank(message = "Target academic year is required")
    private String targetAcademicYear;

    private String targetSection;

    private List<StudentOverride> studentOverrides;

    @Data
    public static class StudentOverride {
        private String studentId;
        private String action;
        private String reason;
    }
}
