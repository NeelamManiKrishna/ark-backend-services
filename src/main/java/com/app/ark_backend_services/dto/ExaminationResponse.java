package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.Examination;
import com.app.ark_backend_services.model.Examination.ExamStatus;
import com.app.ark_backend_services.model.Examination.ExamType;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class ExaminationResponse {

    private String id;
    private String arkId;
    private String organizationId;
    private String branchId;
    private String name;
    private String academicYear;
    private ExamType examType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private ExamStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExaminationResponse from(Examination exam) {
        ExaminationResponse response = new ExaminationResponse();
        response.setId(exam.getId());
        response.setArkId(exam.getArkId());
        response.setOrganizationId(exam.getOrganizationId());
        response.setBranchId(exam.getBranchId());
        response.setName(exam.getName());
        response.setAcademicYear(exam.getAcademicYear());
        response.setExamType(exam.getExamType());
        response.setStartDate(exam.getStartDate());
        response.setEndDate(exam.getEndDate());
        response.setDescription(exam.getDescription());
        response.setStatus(exam.getStatus());
        response.setCreatedAt(exam.getCreatedAt());
        response.setUpdatedAt(exam.getUpdatedAt());
        return response;
    }
}
