package com.app.ark_backend_services.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class PromotionExecuteRequest {

    @NotBlank(message = "Source class ID is required")
    private String sourceClassId;

    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Target academic year must look like 2026-2027")
    private String targetAcademicYear;

    /** Alternative to targetAcademicYear: id from the academic year catalog. */
    private String targetAcademicYearId;

    private String targetSection;

    @Valid
    private List<StudentOverride> studentOverrides;

    @Data
    public static class StudentOverride {
        @NotBlank(message = "Override studentId is required")
        private String studentId;

        @NotNull(message = "Override action is required (PROMOTE, HOLD_BACK or GRADUATE)")
        private Action action;
        private String reason;
    }

    public enum Action {
        PROMOTE, HOLD_BACK, GRADUATE
    }
}
