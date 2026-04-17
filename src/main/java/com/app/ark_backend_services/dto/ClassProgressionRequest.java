package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ClassProgressionRequest {

    @NotEmpty(message = "Sequence cannot be empty")
    private List<ClassLevelDto> sequence;

    @Data
    public static class ClassLevelDto {
        private String className;
        private Integer displayOrder;
        private Boolean isTerminal;
    }
}
