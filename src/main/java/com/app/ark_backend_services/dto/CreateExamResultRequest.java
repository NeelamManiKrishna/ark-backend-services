package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class CreateExamResultRequest {

    @NotBlank
    private String studentId;

    @NotNull
    @PositiveOrZero
    private Double marksObtained;

    private String remarks;
}
