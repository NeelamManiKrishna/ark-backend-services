package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.StudentEnrollment.ExitReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CloseEnrollmentRequest {

    @NotNull(message = "Exit reason is required")
    private ExitReason exitReason;

    private LocalDate exitedAt;
}
