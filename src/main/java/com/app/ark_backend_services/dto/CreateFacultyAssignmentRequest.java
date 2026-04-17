package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.FacultyAssignment.AssignmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateFacultyAssignmentRequest {

    @NotBlank(message = "Faculty ID is required")
    private String facultyId;

    @NotBlank(message = "Class ID is required")
    private String classId;

    private String subjectName;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotNull(message = "Assignment type is required")
    private AssignmentType assignmentType;
}
