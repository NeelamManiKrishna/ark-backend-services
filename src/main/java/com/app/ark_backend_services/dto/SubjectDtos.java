package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Subject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** Request/response shapes for the subject catalog. */
public final class SubjectDtos {
    private SubjectDtos() {}

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Code is required")
        @Size(max = 30, message = "Code is too long")
        private String code;
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name is too long")
        private String name;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 30, message = "Code is too long")
        private String code;
        @Size(max = 120, message = "Name is too long")
        private String name;
    }

    @Data
    @Builder
    public static class Response {
        private String id;
        private String organizationId;
        private String code;
        private String name;
        private Instant createdAt;
        private Instant updatedAt;

        public static Response from(Subject s) {
            return Response.builder().id(s.getId()).organizationId(s.getOrganizationId()).code(s.getCode())
                    .name(s.getName()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt()).build();
        }
    }
}
