package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.StudentEnrollment;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import com.app.ark_backend_services.model.StudentEnrollment.ExitReason;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class StudentEnrollmentResponse {

    private String id;
    private String arkId;
    private String organizationId;
    private String branchId;
    private String studentId;
    private String classId;
    private String academicYear;
    private LocalDate enrolledAt;
    private LocalDate exitedAt;
    private ExitReason exitReason;
    private EnrollmentStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static StudentEnrollmentResponse from(StudentEnrollment enrollment) {
        StudentEnrollmentResponse response = new StudentEnrollmentResponse();
        response.setId(enrollment.getId());
        response.setArkId(enrollment.getArkId());
        response.setOrganizationId(enrollment.getOrganizationId());
        response.setBranchId(enrollment.getBranchId());
        response.setStudentId(enrollment.getStudentId());
        response.setClassId(enrollment.getClassId());
        response.setAcademicYear(enrollment.getAcademicYear());
        response.setEnrolledAt(enrollment.getEnrolledAt());
        response.setExitedAt(enrollment.getExitedAt());
        response.setExitReason(enrollment.getExitReason());
        response.setStatus(enrollment.getStatus());
        response.setCreatedAt(enrollment.getCreatedAt());
        response.setUpdatedAt(enrollment.getUpdatedAt());
        return response;
    }
}
