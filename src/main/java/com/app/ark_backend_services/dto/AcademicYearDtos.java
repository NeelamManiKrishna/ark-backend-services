package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.AcademicYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/** Request/response shapes for the academic year catalog. */
public final class AcademicYearDtos {
    private AcademicYearDtos() {}

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Label is required")
        @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Label must look like 2025-2026")
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
    }

    @Data
    public static class UpdateRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
    }

    @Data
    @Builder
    public static class Response {
        private String id;
        private String organizationId;
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean current;
        private Instant createdAt;
        private Instant updatedAt;

        public static Response from(AcademicYear y) {
            return Response.builder().id(y.getId()).organizationId(y.getOrganizationId()).label(y.getLabel())
                    .startDate(y.getStartDate()).endDate(y.getEndDate()).current(y.isCurrent())
                    .createdAt(y.getCreatedAt()).updatedAt(y.getUpdatedAt()).build();
        }
    }
}
