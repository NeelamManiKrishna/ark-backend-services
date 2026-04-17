package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.ExamResult;
import com.app.ark_backend_services.model.ExamResult.ResultStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class ExamResultResponse {

    private String id;
    private String examinationId;
    private String examSubjectId;
    private String organizationId;
    private String branchId;
    private String classId;
    private String studentId;
    private Double marksObtained;
    private String grade;
    private String remarks;
    private ResultStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExamResultResponse from(ExamResult er) {
        ExamResultResponse response = new ExamResultResponse();
        response.setId(er.getId());
        response.setExaminationId(er.getExaminationId());
        response.setExamSubjectId(er.getExamSubjectId());
        response.setOrganizationId(er.getOrganizationId());
        response.setBranchId(er.getBranchId());
        response.setClassId(er.getClassId());
        response.setStudentId(er.getStudentId());
        response.setMarksObtained(er.getMarksObtained());
        response.setGrade(er.getGrade());
        response.setRemarks(er.getRemarks());
        response.setStatus(er.getStatus());
        response.setCreatedAt(er.getCreatedAt());
        response.setUpdatedAt(er.getUpdatedAt());
        return response;
    }
}
