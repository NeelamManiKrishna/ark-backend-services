package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.FacultyAssignment;
import com.app.ark_backend_services.model.FacultyAssignment.AssignmentStatus;
import com.app.ark_backend_services.model.FacultyAssignment.AssignmentType;
import lombok.Data;

import java.time.Instant;

@Data
public class FacultyAssignmentResponse {

    private String id;
    private String arkId;
    private String organizationId;
    private String branchId;
    private String facultyId;
    private String classId;
    private String subjectName;
    private String academicYear;
    private AssignmentType assignmentType;
    private AssignmentStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static FacultyAssignmentResponse from(FacultyAssignment assignment) {
        FacultyAssignmentResponse response = new FacultyAssignmentResponse();
        response.setId(assignment.getId());
        response.setArkId(assignment.getArkId());
        response.setOrganizationId(assignment.getOrganizationId());
        response.setBranchId(assignment.getBranchId());
        response.setFacultyId(assignment.getFacultyId());
        response.setClassId(assignment.getClassId());
        response.setSubjectName(assignment.getSubjectName());
        response.setAcademicYear(assignment.getAcademicYear());
        response.setAssignmentType(assignment.getAssignmentType());
        response.setStatus(assignment.getStatus());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());
        return response;
    }
}
