package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.FacultyAssignment.AssignmentStatus;
import com.app.ark_backend_services.model.FacultyAssignment.AssignmentType;
import lombok.Data;

@Data
public class UpdateFacultyAssignmentRequest {

    private AssignmentType assignmentType;
    private AssignmentStatus status;
}
